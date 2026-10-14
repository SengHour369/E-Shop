import { useEffect, useState } from 'react';
import { Link, useParams } from 'react-router-dom';
import { Button, PageHeader, Badge } from '@/components/ui';
import { Card } from '@/components/ui/Card';
import { productService } from '@/features/products/services/product.service';
import { brandService } from '@/features/brands/services/brand.service';
import PageContainer from '@/components/layouts/PageContainer';
import { ProductDetailSkeleton } from '@/components/ui/Skeleton';

export default function ProductDetailPage() {
  const { id } = useParams();
  const [product, setProduct] = useState(null);
  const [loading, setLoading] = useState(true);
  const [error, setError] = useState('');

  useEffect(() => {
    let mounted = true;
    const fetchProduct = async () => {
      try {
        setLoading(true);
        const res = await productService.getAll({
          page: 1,
          size: 1,
          criteria_type: 5,
          criteria_value: String(id),
        });
        
        if (mounted) {
          let prod = null;
          // Support both res.data (if single object) or res.data.payload (if list)
          if (res.data && res.data.id) {
            prod = res.data;
          } else if (res.data?.payload && res.data.payload.length > 0) {
            prod = res.data.payload[0];
          }

          if (prod) {
            if (prod.subcategoryId) {
              try {
                const brandRes = await brandService.getAll(1, 1000);
                const data = brandRes?.data?.payload || brandRes?.data?.content || brandRes?.data || [];
                const bList = Array.isArray(data) ? data : (data.payload || []);
                const found = bList.find(b => String(b.id) === String(prod.subcategoryId));
                if (found) {
                  prod.resolvedBrand = found.name;
                  prod.resolvedCategory = found.category_name;
                }
              } catch (e) {
                console.error("Failed to load brand data", e);
              }
            }
            setProduct(prod);
          } else {
            setError('Product not found.');
          }
        }
      } catch (err) {
        if (mounted) {
          setError(err?.response?.data?.message || err?.message || 'Failed to load product details.');
        }
      } finally {
        if (mounted) setLoading(false);
      }
    };

    fetchProduct();

    return () => {
      mounted = false;
    };
  }, [id]);

  if (loading) return <ProductDetailSkeleton />;

  if (error) {
    return (
      <PageContainer>
        <PageHeader
          title="Product details"
          crumbs={[
            { label: 'Dashboard', path: '/dashboard' },
            { label: 'Products', path: '/dashboard/products' },
            { label: 'Details' }
          ]}
        />
        <div className="rounded-lg border border-red-200 bg-red-50 text-red-700 px-4 py-3 text-sm">
          {error}
        </div>
        <Link to="/dashboard/products">
          <Button variant="secondary">Back to Products</Button>
        </Link>
      </PageContainer>
    );
  }

  if (!product) return null;

  return (
    <PageContainer className="space-y-6">
      <div className="flex flex-col sm:flex-row sm:items-center justify-between gap-4">
        <PageHeader
          title="Product details"
          crumbs={[
            { label: 'Dashboard', path: '/dashboard' },
            { label: 'Products', path: '/dashboard/products' },
            { label: product.name }
          ]}
        />
        <div className="flex items-center gap-3">
          <Link to="/dashboard/products">
            <Button variant="secondary">← Back to Products</Button>
          </Link>
          <Link to={`/dashboard/products/edit/${id}`}>
            <Button>Edit product</Button>
          </Link>
        </div>
      </div>

      <div className="grid grid-cols-1 xl:grid-cols-3 gap-6">
        <div className="col-span-1 xl:col-span-2 space-y-6">
          <Card title="General Information">
            <div className="space-y-4">
              <div>
                <h3 className="text-sm font-medium text-slate-500">Name</h3>
                <p className="mt-1 text-base font-medium text-slate-900">{product.name}</p>
              </div>
              <div>
                <h3 className="text-sm font-medium text-slate-500">Description</h3>
                <p className="mt-1 text-sm text-slate-700 whitespace-pre-line">
                  {product.description || 'No description provided.'}
                </p>
              </div>
              <div className="grid grid-cols-2 gap-4">
                <div>
                  <h3 className="text-sm font-medium text-slate-500">Created At</h3>
                  <p className="mt-1 text-sm text-slate-900">
                    {product.createdAt ? new Date(product.createdAt).toLocaleString() : (product.created_at || 'N/A')}
                  </p>
                </div>
                <div>
                  <h3 className="text-sm font-medium text-slate-500">Updated At</h3>
                  <p className="mt-1 text-sm text-slate-900">
                    {product.updated_at ? new Date(product.updated_at).toLocaleString() : 'N/A'}
                  </p>
                </div>
              </div>
            </div>
          </Card>

          <Card title="Variants (SKUs)">
            {product.skus && product.skus.length > 0 ? (
              <div className="space-y-4">
                {product.skus.map((sku, idx) => (
                  <div key={sku.id || idx} className="rounded-xl border border-slate-200 p-5 bg-white shadow-sm">
                    <div className="flex flex-wrap items-center justify-between gap-2 mb-4">
                      <div className="font-semibold text-slate-800 text-lg">
                        {sku.description || sku.sku || `SKU ${idx + 1}`}
                      </div>
                      {sku.is_default ? (
                        <Badge variant="success" dot>Default Variant</Badge>
                      ) : null}
                    </div>
                    
                    <div className="grid grid-cols-2 sm:grid-cols-3 gap-4 mb-4">
                      <div className="bg-slate-50 p-3 rounded-lg border border-slate-100">
                        <span className="block text-slate-500 text-xs uppercase tracking-wider mb-1">Price</span>
                        <span className="font-semibold text-slate-900 text-lg">${Number(sku.price).toFixed(2)}</span>
                      </div>
                      <div className="bg-slate-50 p-3 rounded-lg border border-slate-100">
                        <span className="block text-slate-500 text-xs uppercase tracking-wider mb-1">Inventory</span>
                        <span className="font-semibold text-slate-900 text-lg">{sku.quantity || 0}</span>
                      </div>
                      <div className="bg-slate-50 p-3 rounded-lg border border-slate-100 sm:col-span-1 col-span-2">
                        <span className="block text-slate-500 text-xs uppercase tracking-wider mb-1">Warehouse</span>
                        <span className="font-semibold text-slate-900">{sku.inventory?.warehouse_location || 'N/A'}</span>
                      </div>
                    </div>
                    
                    {sku.product_attributes && sku.product_attributes.length > 0 && (
                      <div className="pt-4 border-t border-slate-100">
                        <span className="block text-slate-700 font-medium text-sm mb-3">Attributes</span>
                        <div className="flex flex-wrap gap-2">
                          {sku.product_attributes.map((attr, aIdx) => (
                            <div key={aIdx} className="inline-flex items-center text-xs bg-indigo-50 text-indigo-700 border border-indigo-100 px-2.5 py-1.5 rounded-md">
                              <span className="font-semibold mr-1">{attr.name}:</span>
                              <span>{attr.attributes?.map(v => v.value).join(', ') || ''}</span>
                            </div>
                          ))}
                        </div>
                      </div>
                    )}
                  </div>
                ))}
              </div>
            ) : (
              <div className="text-center py-6 px-4 border-2 border-dashed border-slate-200 rounded-lg bg-slate-50">
                <p className="text-sm text-slate-500">No variants available.</p>
              </div>
            )}
          </Card>
        </div>

        <div className="col-span-1 space-y-6">
          <Card title="Status & Organization">
            <div className="space-y-5">
              <div>
                <h3 className="text-sm font-medium text-slate-500 mb-2">Status</h3>
                {product.is_active ? (
                  <Badge variant="success" dot>Active</Badge>
                ) : (
                  <Badge variant="danger" dot>Inactive</Badge>
                )}
              </div>
              
              <div className="pt-4 border-t border-slate-100">
                <h3 className="text-sm font-medium text-slate-500 mb-1">Category</h3>
                <p className="text-sm font-medium text-slate-900">
                  {product.resolvedCategory || product.category?.name || product.sub_category?.category?.name || 'Uncategorized'}
                </p>
              </div>

              <div className="pt-4 border-t border-slate-100">
                <h3 className="text-sm font-medium text-slate-500 mb-1">Sub-Category / Brand</h3>
                <p className="text-sm font-medium text-slate-900">
                  {product.resolvedBrand || product.brand?.name || product.sub_category?.name || 'N/A'}
                </p>
              </div>
            </div>
          </Card>

          <Card title="Product Images">
            {product.main_image && product.main_image.length > 0 ? (
              <div className="grid grid-cols-2 gap-3">
                {product.main_image.map((url, i) => (
                  <div key={i} className="aspect-square rounded-lg overflow-hidden border border-slate-200 bg-slate-50">
                    <img src={url} alt={`${product.name} - ${i + 1}`} className="w-full h-full object-cover" />
                  </div>
                ))}
              </div>
            ) : (
              <div className="text-center py-6 px-4 border-2 border-dashed border-slate-200 rounded-lg bg-slate-50">
                <p className="text-sm text-slate-500">No images uploaded</p>
              </div>
            )}
          </Card>
        </div>
      </div>
    </PageContainer>
  );
}
