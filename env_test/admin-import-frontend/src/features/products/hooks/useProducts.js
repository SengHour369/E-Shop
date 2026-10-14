import { useEffect, useRef, useState } from 'react';
import { useNavigate } from 'react-router-dom';
import { productService as productApi } from '@/features/products/services/product.service';
import { subCategoryApi } from '../../brands/services/brand.service';
import { useCategories } from '../../categories/hooks/useCategories';
import { useSubCategories } from '../../subcategorie/hooks/useSubCategories';
import Swal from 'sweetalert2';

const CRITERIA_TYPE_PRODUCT_ID = 5;

const uid = () => `${Date.now()}-${Math.random().toString(36).slice(2, 8)}`;

const createAttributeGroup = () => ({
    id: uid(),
    name: '',
    values: [''],
});

const createVariant = (isDefault = false) => ({
    id: uid(),
    productSkuId: null,
    description: '',
    barcode: '',
    price: '',
    inventory_quantity: '',
    warehouse_location: '',
    is_default: isDefault,
    operatorProductAttribute: false,
    product_attributes: [],
});

const parseOptionalNumber = (value) => {
    if (value === '' || value == null) return undefined;
    const parsed = Number(value);
    return Number.isNaN(parsed) ? undefined : parsed;
};

const buildProductAttributes = (productAttributes) =>
    productAttributes
        .map((attribute) => {
            const name = attribute.name.trim();
            const values = attribute.values
                .map(value => value.trim())
                .filter(Boolean)
                .map(value => ({ id: null, value }));

            if (!name || values.length === 0) {
                return null;
            }

            return { id: null, name, attributes: values };
        })
        .filter(Boolean);

const buildSkuPayload = (variant) => ({
    productSkuId: variant.productSkuId ?? null,
    description: variant.description.trim(),
    barcode: variant.barcode == null ? undefined : variant.barcode.trim(),
    price: parseOptionalNumber(variant.price),
    quantity: parseOptionalNumber(variant.inventory_quantity),
    is_default: variant.is_default,
    inventory: {
        quantity: parseOptionalNumber(variant.inventory_quantity),
        warehouse_location: variant.warehouse_location?.trim() || undefined
    },
    operatorProductAttribute: variant.operatorProductAttribute,
    product_attributes: buildProductAttributes(variant.product_attributes),
});

/** Convert a raw API sku → form variant shape (edit mode hydration) */
const apiSkuToVariant = (sku) => ({
    id: uid(),
    productSkuId: sku.id ?? null,
    description: sku.description || sku.sku || '',
    barcode: sku.barcode || '',
    price: String(sku.price ?? ''),
    inventory_quantity: String(sku.inventory?.quantity ?? sku.quantity ?? ''),
    warehouse_location: sku.inventory?.warehouse_location || '',
    is_default: !!sku.is_default,
    operatorProductAttribute:
        Array.isArray(sku.product_attributes) && sku.product_attributes.length > 0,
    product_attributes: Array.isArray(sku.product_attributes)
        ? sku.product_attributes.map((attribute) => ({
            id: uid(),
            name: attribute.name || '',
            values:
                Array.isArray(attribute.attributes) && attribute.attributes.length > 0
                    ? attribute.attributes.map((a) => a.value || '')
                    : [''],
        }))
        : [],
});

export function useProducts(productId = null) {
    const navigate = useNavigate();
    const isEditMode = Boolean(productId);

    // Categories / sub-categories
    const {
        categories,
        loading: loadingCategories,
        error: categoryError,
        selectedCategoryId,
        setSelectedCategoryId,
        handleCategoryChange: onCategoryChange,
    } = useCategories();
    
    const [initialSubCategoryId, setInitialSubCategoryId] = useState(null);

    const {
        subCategories,
        loading: loadingSubCategories,
        selectedSubCategoryId,
        handleSubCategoryChange: onSubCategoryChange,
    } = useSubCategories(selectedCategoryId, initialSubCategoryId);

    // Page state (only meaningful in edit mode)
    const [pageLoading, setPageLoading] = useState(isEditMode);
    const [pageError, setPageError] = useState('');
    const [existingImages, setExistingImages] = useState([]);

    // Submission state
    const [submitting, setSubmitting] = useState(false);

    // Images (new uploads)
    const [productImages, setProductImages] = useState([]);

    // Variants (SKUs)
    const [variants, setVariants] = useState([createVariant(true)]);

    // Status
    const [isActive, setIsActive] = useState(true);

    // Basic fields
    const [form, setForm] = useState({
        name: '',
        description: '',
    });

    const clearFeedback = () => {};

    // ---- Edit-mode hydration ----
    useEffect(() => {
        if (!productId) return;
        if (categories.length === 0) return; // Wait for categories to load

        let cancelled = false;

        (async () => {
            setPageLoading(true);
            setPageError('');
            try {
                const res = await productApi.getAll({
                    page: 1,
                    size: 1,
                    criteria_type: CRITERIA_TYPE_PRODUCT_ID,
                    criteria_value: String(productId),
                });

                if (cancelled) return;

                const p = res.data;
                if (!p || !p.id) {
                    setPageError('Product not found.');
                    return;
                }

                setForm({ name: p.name || '', description: p.description || '' });
                setIsActive(p.is_active ?? true);
                setExistingImages(Array.isArray(p.main_image) ? p.main_image : []);

                if (p.skus && p.skus.length > 0) {
                    setVariants(p.skus.map(apiSkuToVariant));
                }

                const rawSubCategoryId =
                    p.sub_category?.id ?? p.subCategory?.id ?? p.sub_category_id ??
                    p.subCategoryId ?? p.subcategory_id ?? p.subcategoryId ?? null;

                const rawCategoryId =
                    p.category?.id ?? p.category_id ?? p.categoryId ??
                    p.sub_category?.category?.id ?? p.subCategory?.category?.id ?? null;

                if (rawCategoryId != null) {
                    if (rawSubCategoryId != null) {
                        setInitialSubCategoryId(String(rawSubCategoryId));
                    }
                    setSelectedCategoryId(String(rawCategoryId));
                } else if (rawSubCategoryId != null) {
                    try {
                        const subRes = await subCategoryApi.getAll(1, 1000);
                        const data = subRes?.data?.payload || subRes?.data?.content || subRes?.data || [];
                        const bList = Array.isArray(data) ? data : (data.payload || []);
                        const found = bList.find(b => String(b.id) === String(rawSubCategoryId));

                        let parentCategoryId =
                            found?.category?.id ?? found?.category_id ?? found?.categoryId ?? null;

                        if (parentCategoryId == null && found?.category_name) {
                            const matched = categories.find(c => c.name === found.category_name);
                            if (matched) parentCategoryId = matched.id;
                        }

                        if (parentCategoryId != null) {
                            setInitialSubCategoryId(String(rawSubCategoryId));
                            setSelectedCategoryId(String(parentCategoryId));
                        }
                    } catch (subErr) {
                        console.error('[useProducts] failed to hydrate sub-category:', subErr);
                    }
                }
            } catch (err) {
                if (!cancelled) setPageError(err?.message || 'Failed to load product.');
            } finally {
                if (!cancelled) setPageLoading(false);
            }
        })();

        return () => { cancelled = true; };
    }, [productId, setSelectedCategoryId, categories]);

    // ---- Basic field handlers ----
    const handleStatusChange = (e) => {
        setIsActive(e.target.value === 'true');
        clearFeedback();
    };

    const updateField = (key, value) => {
        setForm(prev => ({ ...prev, [key]: value }));
        clearFeedback();
    };

    // ---- Variant handlers ----
    const updateVariant = (variantId, key, value) => {
        setVariants(prev => prev.map(variant => (
            variant.id === variantId ? { ...variant, [key]: value } : variant
        )));
        clearFeedback();
    };

    const addVariant = () => {
        setVariants(prev => [...prev, createVariant(prev.length === 0)]);
        clearFeedback();
    };

    const removeVariant = (variantId) => {
        setVariants(prev => {
            const next = prev.filter(variant => variant.id !== variantId);
            if (next.length === 0) {
                return [createVariant(true)];
            }

            if (!next.some(variant => variant.is_default)) {
                next[0] = { ...next[0], is_default: true };
            }

            return next;
        });
        clearFeedback();
    };

    const setDefaultVariant = (variantId) => {
        setVariants(prev => prev.map(variant => ({
            ...variant,
            is_default: variant.id === variantId,
        })));
        clearFeedback();
    };

    // ---- Attribute group handlers (per variant) ----
    const addAttributeGroup = (variantId) => {
        setVariants(prev => prev.map(variant => (
            variant.id === variantId
                ? {
                    ...variant,
                    operatorProductAttribute: true,
                    product_attributes: [...variant.product_attributes, createAttributeGroup()],
                }
                : variant
        )));
        clearFeedback();
    };

    const removeAttributeGroup = (variantId, attributeId) => {
        setVariants(prev => prev.map(variant => {
            if (variant.id !== variantId) return variant;
            return {
                ...variant,
                product_attributes: variant.product_attributes.filter(attribute => attribute.id !== attributeId),
            };
        }));
        clearFeedback();
    };

    const updateVariantAttribute = (variantId, attributeId, key, value) => {
        setVariants(prev => prev.map(variant => {
            if (variant.id !== variantId) return variant;
            return {
                ...variant,
                product_attributes: variant.product_attributes.map(attribute => (
                    attribute.id === attributeId ? { ...attribute, [key]: value } : attribute
                )),
            };
        }));
        clearFeedback();
    };

    const handleAttributeToggle = (variantId, enabled) => {
        setVariants(prev => prev.map(variant => {
            if (variant.id !== variantId) return variant;
            return {
                ...variant,
                operatorProductAttribute: enabled,
                product_attributes: enabled
                    ? (variant.product_attributes.length > 0 ? variant.product_attributes : [createAttributeGroup()])
                    : [],
            };
        }));
        clearFeedback();
    };

    // ---- Attribute value handlers (per attribute group) ----
    const addAttributeValue = (variantId, attributeId) => {
        setVariants(prev => prev.map(variant => {
            if (variant.id !== variantId) return variant;
            return {
                ...variant,
                product_attributes: variant.product_attributes.map(attribute => (
                    attribute.id === attributeId
                        ? { ...attribute, values: [...attribute.values, ''] }
                        : attribute
                )),
            };
        }));
        clearFeedback();
    };

    const updateAttributeValue = (variantId, attributeId, valueIndex, value) => {
        setVariants(prev => prev.map(variant => {
            if (variant.id !== variantId) return variant;
            return {
                ...variant,
                product_attributes: variant.product_attributes.map(attribute => {
                    if (attribute.id !== attributeId) return attribute;
                    return {
                        ...attribute,
                        values: attribute.values.map((item, index) => (index === valueIndex ? value : item)),
                    };
                }),
            };
        }));
        clearFeedback();
    };

    const removeAttributeValue = (variantId, attributeId, valueIndex) => {
        setVariants(prev => prev.map(variant => {
            if (variant.id !== variantId) return variant;
            return {
                ...variant,
                product_attributes: variant.product_attributes.map(attribute => {
                    if (attribute.id !== attributeId) return attribute;
                    const nextValues = attribute.values.filter((_, index) => index !== valueIndex);
                    return {
                        ...attribute,
                        values: nextValues.length > 0 ? nextValues : [''],
                    };
                }),
            };
        }));
        clearFeedback();
    };

    // ---- Category / sub-category change handlers ----
    const handleCategoryChange = (e) => {
        onCategoryChange(e);
        clearFeedback();
    };

    const handleSubCategoryChange = (e) => {
        onSubCategoryChange(e);
        clearFeedback();
    };

    // ---- Navigation ----
    const handleCancel = () => {
        navigate(isEditMode ? -1 : '/dashboard/products');
    };

    // ---- Validation ----
    const validate = () => {
        if (!form.name.trim()) {
            return 'Product name is required.';
        }

        if (!isEditMode && productImages.length === 0) {
            return 'Please upload at least one product image.';
        }

        if (!selectedSubCategoryId) {
            return 'Please choose a sub-category.';
        }

        const firstInvalidVariant = variants.find((variant) => {
            return !variant.description.trim()
                || parseOptionalNumber(variant.price) == null
        });

        if (firstInvalidVariant) {
            return 'Each SKU needs a description, price';
        }

        const invalidAttributeVariant = variants.find((variant) => {
            if (!variant.operatorProductAttribute) return false;
            return variant.product_attributes.some((attribute) => {
                if (!attribute.name.trim()) return true;
                return attribute.values.some(value => !value.trim());
            });
        });

        if (invalidAttributeVariant) {
            return 'Fill in every enabled product attribute name and value, or turn the switch off.';
        }

        return null;
    };

    const resetForm = () => {
        setForm({ name: '', description: '' });
        setProductImages([]);
        setVariants([createVariant(true)]);
        setSelectedCategoryId('');
        setIsActive(true);
    };

    const handleSubmit = async (e) => {
        e.preventDefault();

        const validationError = validate();
        if (validationError) {
            Swal.fire({
                toast: true,
                position: 'top-end',
                showConfirmButton: false,
                timer: 3000,
                icon: 'warning',
                title: validationError,
            });
            return;
        }

        const normalizedVariants = variants.map(buildSkuPayload);

        if (!normalizedVariants.some(variant => variant.is_default)) {
            Swal.fire({
                toast: true,
                position: 'top-end',
                showConfirmButton: false,
                timer: 3000,
                icon: 'warning',
                title: 'Choose one default SKU.',
            });
            return;
        }

        const payload = {
            name: form.name.trim(),
            description: form.description.trim() || undefined,
            is_active: isActive,
            sub_category_id: Number(selectedSubCategoryId),
            skus: normalizedVariants,
            files: productImages,
        };

        try {
            setSubmitting(true);

            if (isEditMode) {
                await productApi.update(productId, payload);
                Swal.fire({
                    toast: true,
                    position: 'top-end',
                    showConfirmButton: false,
                    timer: 3000,
                    icon: 'success',
                    title: 'Product updated successfully',
                });
                setProductImages([]);
                setTimeout(() => navigate(`/dashboard/products/view/${productId}`), 1200);
            } else {
                await productApi.create(payload);
                Swal.fire({
                    toast: true,
                    position: 'top-end',
                    showConfirmButton: false,
                    timer: 3000,
                    icon: 'success',
                    title: 'Product created successfully',
                });
                resetForm();
            }
        } catch (err) {
            const message = err?.response?.data?.message || err?.message || `Failed to ${isEditMode ? 'update' : 'create'} product.`;
            Swal.fire({
                toast: true,
                position: 'top-end',
                showConfirmButton: false,
                timer: 3000,
                icon: 'error',
                title: message,
            });
        } finally {
            setSubmitting(false);
        }
    };

    return {
        isEditMode,

        // edit-mode page state
        pageLoading,
        pageError,
        existingImages,

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
    };
}

export default useProducts;
