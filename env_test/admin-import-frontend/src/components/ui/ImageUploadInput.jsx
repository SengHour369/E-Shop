import React, { useEffect, useState } from 'react';
import { cn } from '@/utils/cn';

export default function ImageUploadInput({ className, files = [], onFilesChange }) {
  const [images, setImages] = useState([]);

  useEffect(() => {
    const nextImages = files.map(file => ({
      file,
      preview: URL.createObjectURL(file),
    }));

    setImages(nextImages);

    return () => {
      nextImages.forEach(item => URL.revokeObjectURL(item.preview));
    };
  }, [files]);

  const handleFileChange = (e) => {
    if (e.target.files) {
      const nextFiles = [...files, ...Array.from(e.target.files)];
      onFilesChange?.(nextFiles);
    }
  };

  const removeImage = (index) => {
    const nextFiles = files.filter((_, i) => i !== index);
    onFilesChange?.(nextFiles);
  };

  return (
    <div className={cn("space-y-4", className)}>
      <div className="flex items-center justify-center w-full">
        <label className="flex flex-col items-center justify-center w-full h-48 border-2 border-slate-300 border-dashed rounded-xl cursor-pointer bg-slate-50 hover:bg-slate-100 transition-colors">
          <div className="flex flex-col items-center justify-center pt-5 pb-6">
            <svg className="w-10 h-10 mb-3 text-slate-400" fill="none" stroke="currentColor" viewBox="0 0 24 24" xmlns="http://www.w3.org/2000/svg">
              <path strokeLinecap="round" strokeLinejoin="round" strokeWidth="2" d="M7 16a4 4 0 01-.88-7.903A5 5 0 1115.9 6L16 6a5 5 0 011 9.9M15 13l-3-3m0 0l-3 3m3-3v12"></path>
            </svg>
            <p className="mb-2 text-sm text-slate-500"><span className="font-semibold">Click to upload</span> or drag and drop</p>
            <p className="text-xs text-slate-500">SVG, PNG, JPG or GIF (MAX. 800x400px)</p>
          </div>
          <input type="file" className="hidden" multiple accept="image/*" onChange={handleFileChange} />
        </label>
      </div>

      {images.length > 0 && (
        <div className="grid grid-cols-3 gap-4">
          {images.map((src, idx) => (
            <div key={idx} className="relative rounded-lg overflow-hidden border border-slate-200 aspect-square">
              <img src={src.preview} alt={`Upload ${idx}`} className="object-cover w-full h-full" />
              <button 
                type="button"
                className="absolute top-2 right-2 bg-red-500 text-white rounded-full p-1 hover:bg-red-600 focus:outline-none"
                onClick={() => removeImage(idx)}
              >
                <svg className="w-4 h-4" fill="none" stroke="currentColor" viewBox="0 0 24 24">
                  <path strokeLinecap="round" strokeLinejoin="round" strokeWidth="2" d="M6 18L18 6M6 6l12 12"></path>
                </svg>
              </button>
            </div>
          ))}
        </div>
      )}
    </div>
  );
}
