package com.example.eshop.common.imports;
import org.springframework.web.multipart.MultipartFile;
import org.springframework.web.server.ResponseStatusException;
import org.springframework.http.HttpStatus;
import javax.imageio.*;
import java.awt.image.BufferedImage;
import java.io.*;
import java.nio.file.Files;
import java.security.MessageDigest;
import java.util.*;
/** Bounded literal PNG/JPEG data; strips metadata and never accepts SVG or remote URLs. */
public final class ImportImageFile implements MultipartFile {
 private static final java.util.concurrent.Semaphore slots=new java.util.concurrent.Semaphore(2);
 private final byte[] bytes;private final String filename;
 public ImportImageFile(String filename,byte[] bytes){this.filename=filename;this.bytes=bytes.clone();}
 public static ImportImageFile sanitize(MultipartFile file){
  if(!slots.tryAcquire())throw new ResponseStatusException(HttpStatus.TOO_MANY_REQUESTS,"Image processor busy");
  try{
   if(file.isEmpty()||file.getSize()>5_242_880)throw invalid();
   byte[] raw;try(var stream=file.getInputStream()){raw=stream.readNBytes(5_242_881);}if(raw.length>5_242_880)throw invalid();
   try(var input=ImageIO.createImageInputStream(new ByteArrayInputStream(raw))){
    var readers=ImageIO.getImageReaders(input);if(!readers.hasNext())throw invalid();var reader=readers.next();
    try{reader.setInput(input,true,true);String format=reader.getFormatName().toLowerCase(Locale.ROOT);int w=reader.getWidth(0),h=reader.getHeight(0);
     if(!Set.of("png","jpeg","jpg").contains(format)||w<1||h<1||(long)w*h>10_000_000||w>8192||h>8192)throw invalid();
     BufferedImage image=reader.read(0);var out=new ByteArrayOutputStream();if(!ImageIO.write(image,"png",out)||out.size()>5_242_880)throw invalid();
     return new ImportImageFile("product.png",out.toByteArray());
    }finally{reader.dispose();}
   }
  }catch(ResponseStatusException e){throw e;}catch(Exception e){throw invalid();}finally{slots.release();}
 }
 public String checksum(){try{return HexFormat.of().formatHex(MessageDigest.getInstance("SHA-256").digest(bytes));}catch(Exception e){throw new IllegalStateException(e);}}
 public String getName(){return "files";}public String getOriginalFilename(){return filename;}public String getContentType(){return "image/png";}
 public boolean isEmpty(){return bytes.length==0;}public long getSize(){return bytes.length;}public byte[] getBytes(){return bytes.clone();}
 public InputStream getInputStream(){return new ByteArrayInputStream(bytes);}public void transferTo(File dest)throws IOException{Files.write(dest.toPath(),bytes);}
 private static ResponseStatusException invalid(){return new ResponseStatusException(HttpStatus.BAD_REQUEST,"Upload a valid PNG/JPEG image up to 5 MiB and 10 million pixels");}
}
