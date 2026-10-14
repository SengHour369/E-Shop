import Input, { Textarea, Select, Toggle } from '@/components/ui/Input';
import Button from '@/components/ui/Button';
import ImageUploadInput from '@/components/ui/ImageUploadInput';
import { Card } from '@/components/ui/Card';
import { useProducts } from '@/features/products/hooks/useProducts';
import PageContainer from '@/components/layouts/PageContainer';
import SearchableSelect from '@/components/ui/SearchableSelect';


export default function AddProductForm() {
  const {
    // categories / sub-categories
    categories,
    subCategories,
    selectedCategoryId,
    selectedSubCategoryId,
    loadingCategories,
    loadingSubCategories,
    categoryError,
    handleCategoryChange,
    handleSubCategoryChange,

    // submission state
    submitting,

    // basic fields
    form,
    updateField,
    isActive,
    handleStatusChange,

    // images
    productImages,
    setProductImages,

    // variants
    variants,
    updateVariant,
    addVariant,
    removeVariant,
    setDefaultVariant,

    // attribute groups
    addAttributeGroup,
    removeAttributeGroup,
    updateVariantAttribute,
    handleAttributeToggle,

    // attribute values
    addAttributeValue,
    updateAttributeValue,
    removeAttributeValue,
    // actions
    handleCancel,
    handleSubmit,
  } = useProducts();

  return (
    <PageContainer>
      <div className="flex justify-between items-center mb-6">
        <div>
          <h1 className="text-xl font-bold">Add Product</h1>
          <p className="text-slate-500">Dashboard › Products › Add Product</p>
        </div>
        <div className="flex gap-3">
          <Button type="button" variant="secondary" onClick={handleCancel}>Cancel</Button>
          <Button type="button" onClick={handleSubmit} loading={submitting}>Save Product</Button>
        </div>
      </div>

      <div className="grid grid-cols-12 gap-6">
        <div className="col-span-12 lg:col-span-7 space-y-6">
          <Card title="Basic Information">
            <div className="grid grid-cols-2 gap-4">
              <Input
                label="Product Name *"
                placeholder="Nike Air Max 270"
                className="col-span-2"
                value={form.name}
                onChange={(e) => updateField('name', e.target.value)}
                required
              />

              <div className="form-group">
                <label className="form-label">Category <span className="ml-0.5 text-red-500">*</span></label>
                <SearchableSelect
                  options={categories.map(cat => ({ label: cat.name, value: cat.id }))}
                  value={selectedCategoryId}
                  onChange={(val) => handleCategoryChange({ target: { value: val } })}
                  placeholder={loadingCategories ? 'Loading categories…' : '— Select category —'}
                  disabled={loadingCategories}
                />
                {categoryError && <p className="mt-1 text-xs text-red-600">{categoryError}</p>}
              </div>

              <Textarea
                label="Description *"
                className="col-span-2"
                rows={5}
                value={form.description}
                onChange={(e) => updateField('description', e.target.value)}
              />
              <div className="row-span-2 text-sm text-slate-500">
                <div className="form-group mb-5">
                  <label className="form-label">Sub-Category <span className="ml-0.5 text-red-500">*</span></label>
                  <SearchableSelect
                    options={subCategories.map(sub => ({ label: sub.name, value: sub.id }))}
                    value={selectedSubCategoryId}
                    onChange={(val) => handleSubCategoryChange({ target: { value: val } })}
                    placeholder={
                      !selectedCategoryId
                        ? '— Select a category first —'
                        : loadingSubCategories
                          ? 'Loading…'
                          : subCategories.length === 0
                            ? 'No sub-categories found'
                            : '— Select sub-category —'
                    }
                    disabled={!selectedCategoryId || loadingSubCategories}
                  />
                </div>

                <Select
                  label="Status (Active) *"
                  value={isActive ? 'true' : 'false'}
                  onChange={handleStatusChange}
                >
                  <option value="true">true</option>
                  <option value="false">false</option>
                </Select>
              </div>
            </div>
          </Card>

          <Card title="Product Variants">
            <div className="space-y-6">
              <p className="text-sm text-slate-500">
                Add one or more SKUs for the same product. Each SKU can have its own price, inventory, and attribute set.
              </p>

              <div className="space-y-4">
                {variants.map((variant, index) => (
                  <div key={variant.id} className="rounded-xl border border-slate-200 bg-slate-50 p-4">
                    <div className="mb-4 flex items-center justify-between gap-3">
                      <div>
                        <h3 className="font-semibold text-slate-800">SKU {index + 1}</h3>
                        <p className="text-sm text-slate-500">Use this row to define one sellable variant.</p>
                      </div>
                      <div className="flex items-center gap-3">
                        <label className="flex items-center gap-2 text-sm text-slate-700">
                          <input
                            type="radio"
                            name="default-sku"
                            checked={variant.is_default}
                            onChange={() => setDefaultVariant(variant.id)}
                            className="h-4 w-4 border-slate-300 text-indigo-600 focus:ring-indigo-500"
                          />
                          Default SKU
                        </label>
                        <Button
                          type="button"
                          variant="secondary"
                          onClick={() => removeVariant(variant.id)}
                          disabled={variants.length === 1}
                        >
                          Remove
                        </Button>
                      </div>
                    </div>

                    <div className="grid grid-cols-2 gap-4">
                      <Input
                        label="SKU Description *"
                        placeholder="Black - 128GB"
                        className="col-span-2"
                        value={variant.description}
                        onChange={(e) => updateVariant(variant.id, 'description', e.target.value)}
                      />
                      <Input
                        label="Retail barcode"
                        placeholder="8850123456787"
                        value={variant.barcode || ''}
                        onChange={(e) => updateVariant(variant.id, 'barcode', e.target.value)}
                      />
                      <Input
                        label="Price *"
                        type="number"
                        step="0.01"
                        min="0"
                        placeholder="999.99"
                        value={variant.price}
                        onChange={(e) => updateVariant(variant.id, 'price', e.target.value)}
                      />
                      <Input
                        label="Inventory Quantity *"
                        type="number"
                        min="0"
                        placeholder="50"
                        value={variant.inventory_quantity}
                        onChange={(e) => updateVariant(variant.id, 'inventory_quantity', e.target.value)}
                      />
                      <Input
                        label="Warehouse Location"
                        placeholder="Warehouse A"
                        value={variant.warehouse_location}
                        onChange={(e) => updateVariant(variant.id, 'warehouse_location', e.target.value)}
                      />
                      </div>

                    <div className="mt-5 space-y-3">
                      <label className="flex items-center gap-2 text-sm font-medium text-slate-700">
                        <Toggle
                          id={`variant-attributes-${variant.id}`}
                          checked={variant.operatorProductAttribute}
                          onChange={(checked) => handleAttributeToggle(variant.id, checked)}
                          label="Product Attributes"
                        />
                      </label>

                      {variant.operatorProductAttribute && (
                        <div className="space-y-3">
                          <div className="flex items-center justify-between gap-3">
                            <h4 className="text-sm font-semibold text-slate-800">Product Attributes</h4>
                            <Button type="button" variant="secondary" onClick={() => addAttributeGroup(variant.id)}>
                              Add Attribute
                            </Button>
                          </div>

                          {variant.product_attributes.length === 0 ? (
                            <p className="text-sm text-slate-500">No attributes yet. Turn the switch off to skip attributes.</p>
                          ) : (
                            <div className="space-y-3">
                              {variant.product_attributes.map((attribute) => (
                                <div key={attribute.id} className="rounded-lg border border-slate-200 bg-white p-3">
                                  <div className="grid grid-cols-12 gap-3">
                                    <div className="col-span-12 md:col-span-4">
                                      <Input
                                        label="Attribute Name"
                                        placeholder="Color"
                                        value={attribute.name}
                                        onChange={(e) => updateVariantAttribute(variant.id, attribute.id, 'name', e.target.value)}
                                      />
                                    </div>
                                    <div className="col-span-12 md:col-span-8 md:flex md:items-end md:justify-end">
                                      <Button
                                        type="button"
                                        variant="secondary"
                                        onClick={() => removeAttributeGroup(variant.id, attribute.id)}
                                        className="w-full md:w-auto"
                                      >
                                        Remove Attribute
                                      </Button>
                                    </div>
                                  </div>

                                  <div className="mt-4 space-y-3">
                                    <div className="flex items-center justify-between gap-3">
                                      <h5 className="text-sm font-medium text-slate-700">Values</h5>
                                      <Button type="button" variant="secondary" onClick={() => addAttributeValue(variant.id, attribute.id)}>
                                        Add Value
                                      </Button>
                                    </div>

                                    <div className="space-y-3">
                                      {attribute.values.map((value, valueIndex) => (
                                        <div key={`${attribute.id}-${valueIndex}`} className="flex items-end gap-3">
                                          <div className="flex-3">
                                            <Input
                                              label={valueIndex === 0 ? 'Value' : ''}
                                              placeholder="Black"
                                              value={value}
                                              onChange={(e) => updateAttributeValue(variant.id, attribute.id, valueIndex, e.target.value)}
                                            />
                                          </div>
                                          <Button
                                            type="button"
                                            variant="secondary"
                                            onClick={() => removeAttributeValue(variant.id, attribute.id, valueIndex)}
                                            disabled={attribute.values.length === 1}
                                          >
                                            Remove
                                          </Button>
                                        </div>
                                      ))}
                                    </div>
                                  </div>
                                </div>
                              ))}
                            </div>
                          )}
                        </div>
                      )}
                    </div>
                  </div>
                ))}
              </div>

              <Button type="button" variant="secondary" onClick={addVariant}>
                Add SKU
              </Button>
            </div>
          </Card>
        </div>

        <div className="col-span-12 lg:col-span-5 space-y-6">
          <Card title="Product Images">
            <ImageUploadInput
              files={productImages}
              onFilesChange={setProductImages}
            />
          </Card>
        </div>
      </div>
    </PageContainer>
  );
}
