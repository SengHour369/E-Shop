package com.example.eshop.catalog.scanner;

import com.example.eshop.catalog.dto.response.*;
import com.example.eshop.catalog.model.ProductSku;
import com.example.eshop.catalog.repository.ProductRepository;
import com.example.eshop.catalog.repository.ProductSkuRepository;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.data.domain.PageRequest;
import java.util.*;

/** Visual retrieval across catalog images, independent of AI-generated product names. */
@Service
@RequiredArgsConstructor
public class CatalogPhotoSearch {
    private final ProductRepository products;
    private final ProductSkuRepository skus;
    private final CatalogVisualComparison comparison;
    public ImageMatchResponse search(byte[] photo,String mediaType,String requestId) {
        var matches=new HashMap<Long,ImageCandidate>();
        boolean compared=false,failed=false;
        int page=0;
        org.springframework.data.domain.Page<com.example.eshop.catalog.model.Product> batch;
        do {
            batch=products.findByIsActiveTrue(PageRequest.of(page++,50));
            var ids=batch.getContent().stream().map(p->p.getId()).toList();
            if(ids.isEmpty())break;
            var byProduct=new HashMap<Long,ProductSku>();
            var candidates=new ArrayList<ImageCandidate>();
            var seen=new HashSet<String>();
            for(var sku:skus.findForScannerProducts(ids)) {
                var product=sku.getProduct();var current=byProduct.get(product.getId());
                if(current==null||Boolean.TRUE.equals(sku.getIsDefault()))byProduct.put(product.getId(),sku);
                if(sku.getImage()!=null&&sku.getImage().getUrl()!=null&&seen.add(product.getId()+":"+sku.getImage().getUrl()))
                    candidates.add(new ImageCandidate(product.getId(),sku.getId(),product.getName(),sku.getSku(),sku.getImage().getUrl(),0));
            }
            var names=new HashMap<Long,String>();batch.forEach(p->names.put(p.getId(),p.getName()));
            for(var image:products.findImageUrls(ids)) {
                var sku=byProduct.get(image.getProductId());
                if(image.getUrl()!=null&&seen.add(image.getProductId()+":"+image.getUrl()))
                    candidates.add(new ImageCandidate(image.getProductId(),sku==null?null:sku.getId(),names.get(image.getProductId()),sku==null?null:sku.getSku(),image.getUrl(),0));
            }
            for(int start=0;start<candidates.size();start+=5) {
                var input=ImageMatchResponse.builder().requestId(requestId).candidates(candidates.subList(start,Math.min(start+5,candidates.size()))).build();
                var result=comparison.compare(photo,mediaType,input);
                if(!"AI_VISUAL_COMPARISON".equals(result.getMatchType())) {failed=true;continue;}
                compared=true;
                for(var candidate:result.getCandidates())if(candidate.getConfidence()>=0.65)
                    matches.merge(candidate.getProductId(),candidate,(left,right)->left.getConfidence()>=right.getConfidence()?left:right);
            }
        } while(batch.hasNext());
        if(!compared&&failed)return CatalogImageMatcher.unavailable(requestId);
        var ranked=matches.values().stream().sorted(Comparator.comparingDouble(ImageCandidate::getConfidence).reversed().thenComparing(ImageCandidate::getProductId)).limit(10).toList();
        return ImageMatchResponse.builder().matchType("AI_VISUAL_COMPARISON").exact(false).requiresSelection(!ranked.isEmpty()).requestId(requestId)
            .confidence(ranked.isEmpty()?null:ranked.get(0).getConfidence()).candidates(ranked)
            .message(ranked.isEmpty()?(failed?"Some catalog images could not be compared. Try again.":"No visually similar catalog products found."):(failed?"Similar products found; some catalog images could not be compared.":"Choose from visually similar products")).build();
    }
}
