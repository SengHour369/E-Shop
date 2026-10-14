import { useState } from 'react';
import { Toggle } from './Input';
import Modal from './Modal';

export default function VariantsTable() {
  const [hasVariants, setHasVariants] = useState(true);
  const [attributes, setAttributes] = useState([
    { id: 1, name: 'Size', values: ['S', 'M', 'L', 'XL'] },
    { id: 2, name: 'Color', values: ['Black', 'White', 'Red'] },
  ]);
  
  const [modalState, setModalState] = useState({ isOpen: false, attrId: null, value: '' });

  const addAttribute = () => {
    setAttributes([...attributes, { id: Date.now(), name: '', values: [] }]);
  };

  const removeAttribute = (id) => {
    setAttributes(attributes.filter(attr => attr.id !== id));
  };

  const updateAttributeName = (id, name) => {
    setAttributes(attributes.map(attr => attr.id === id ? { ...attr, name } : attr));
  };

  const openAddValueModal = (id) => {
    setModalState({ isOpen: true, attrId: id, value: '' });
  };

  const handleAddValueConfirm = () => {
    const { attrId, value } = modalState;
    if (value.trim()) {
      setAttributes(attributes.map(attr => attr.id === attrId ? { ...attr, values: [...attr.values, value.trim()] } : attr));
    }
    setModalState({ isOpen: false, attrId: null, value: '' });
  };

  const removeValue = (id, valueIdx) => {
    setAttributes(attributes.map(attr => attr.id === id ? { 
      ...attr, 
      values: attr.values.filter((_, idx) => idx !== valueIdx) 
    } : attr));
  };

  return (
    <div className="space-y-6">
      <div className="flex items-center">
        <Toggle 
          id="has-variants-toggle"
          checked={hasVariants} 
          onChange={setHasVariants} 
          label="This product has variants" 
        />
      </div>

      {hasVariants && (
        <div className="space-y-4">
          <h3 className="font-semibold text-slate-800 text-sm">Variant Attributes</h3>
          
          <div className="space-y-4">
            {attributes.map((attr) => (
              <div key={attr.id} className="flex items-start gap-4">
                {/* Attribute Name */}
                <div className="w-1/4">
                  <select 
                    className="w-full rounded-lg border border-slate-200 bg-white px-3 py-2 text-sm text-slate-900 focus:border-indigo-500 focus:ring-2 focus:ring-indigo-500 outline-none"
                    value={attr.name}
                    onChange={(e) => updateAttributeName(attr.id, e.target.value)}
                  >
                    <option value="" disabled>Select...</option>
                    <option value="Size">Size</option>
                    <option value="Color">Color</option>
                    <option value="Material">Material</option>
                    <option value="Style">Style</option>
                  </select>
                </div>

                {/* Attribute Values */}
                <div className="flex-1">
                  <div className="flex flex-wrap items-center gap-2">
                    {attr.values.map((val, idx) => (
                      <div key={idx} className="flex items-center gap-1 bg-slate-50 border border-slate-200 rounded-md px-2 py-1 text-sm text-slate-700">
                        <span>{val}</span>
                        <button 
                          onClick={() => removeValue(attr.id, idx)}
                          className="text-slate-400 hover:text-slate-600 focus:outline-none"
                        >
                          <svg className="w-3.5 h-3.5" fill="none" stroke="currentColor" viewBox="0 0 24 24">
                            <path strokeLinecap="round" strokeLinejoin="round" strokeWidth="2" d="M6 18L18 6M6 6l12 12"></path>
                          </svg>
                        </button>
                      </div>
                    ))}
                    <button 
                      onClick={() => openAddValueModal(attr.id)}
                      className="flex items-center justify-center border border-slate-200 border-dashed rounded-md px-3 py-1.5 text-sm text-slate-600 hover:border-slate-300 hover:bg-slate-50 transition-colors"
                    >
                      + Add Value
                    </button>
                  </div>
                </div>

                {/* Delete Button */}
                <button 
                  onClick={() => removeAttribute(attr.id)}
                  className="flex-shrink-0 flex items-center justify-center w-9 h-9 border border-slate-200 rounded-lg text-slate-400 hover:text-red-500 hover:border-red-200 hover:bg-red-50 transition-colors"
                >
                  <svg className="w-4 h-4" fill="none" stroke="currentColor" viewBox="0 0 24 24">
                    <path strokeLinecap="round" strokeLinejoin="round" strokeWidth="2" d="M19 7l-.867 12.142A2 2 0 0116.138 21H7.862a2 2 0 01-1.995-1.858L5 7m5 4v6m4-6v6m1-10V4a1 1 0 00-1-1h-4a1 1 0 00-1 1v3M4 7h16"></path>
                  </svg>
                </button>
              </div>
            ))}
          </div>

          <button 
            onClick={addAttribute}
            className="text-sm font-medium text-indigo-600 hover:text-indigo-700 focus:outline-none flex items-center gap-1 mt-2"
          >
            + Add Attribute
          </button>
        </div>
      )}

      {/* Add Value Modal */}
      <Modal
        isOpen={modalState.isOpen}
        onClose={() => setModalState({ isOpen: false, attrId: null, value: '' })}
        title="Add Variant Value"
        size="sm"
        footer={
          <>
            <button 
              onClick={() => setModalState({ isOpen: false, attrId: null, value: '' })}
              className="px-4 py-2 rounded-lg border border-slate-200 text-sm font-medium text-slate-700 hover:bg-slate-50 transition-colors"
            >
              Cancel
            </button>
            <button 
              onClick={handleAddValueConfirm}
              className="px-4 py-2 rounded-lg bg-indigo-600 text-sm font-medium text-white hover:bg-indigo-700 transition-colors"
            >
              Add
            </button>
          </>
        }
      >
        <div className="py-2">
          <label className="block text-sm font-medium text-slate-700 mb-1">Value Name</label>
          <input
            type="text"
            autoFocus
            className="w-full rounded-lg border border-slate-200 bg-white px-3 py-2 text-sm text-slate-900 focus:border-indigo-500 focus:ring-2 focus:ring-indigo-500 outline-none"
            placeholder="e.g. XL, Blue, Cotton..."
            value={modalState.value}
            onChange={(e) => setModalState({ ...modalState, value: e.target.value })}
            onKeyDown={(e) => e.key === 'Enter' && handleAddValueConfirm()}
          />
        </div>
      </Modal>
    </div>
  );
}