import { useState, useRef, useEffect } from 'react';
import { createPortal } from 'react-dom';
import { HiChevronDown, HiMagnifyingGlass } from 'react-icons/hi2';

export default function SearchableSelect({ options, value, onChange, placeholder = "Select an option", disabled = false }) {
  const [isOpen, setIsOpen] = useState(false);
  const [query, setQuery] = useState('');
  const wrapperRef = useRef(null);
  const listRef = useRef(null);

  const [dropdownStyles, setDropdownStyles] = useState({});

  useEffect(() => {
    function handleClickOutside(event) {
      if (wrapperRef.current && !wrapperRef.current.contains(event.target)) {
        // Also check if click is inside the portal dropdown!
        // We'll add an ID to the dropdown to check it easily
        if (!document.getElementById('searchable-select-portal')?.contains(event.target)) {
          setIsOpen(false);
          setQuery('');
        }
      }
    }
    
    function handleScrollOrResize(event) {
      if (isOpen) {
        // Prevent closing if the scroll event originated from inside the portal
        if (
          event.type === 'scroll' &&
          event.target instanceof Node &&
          document.getElementById('searchable-select-portal')?.contains(event.target)
        ) {
          return;
        }
        setIsOpen(false);
        setQuery('');
      }
    }

    document.addEventListener("mousedown", handleClickOutside);
    window.addEventListener("resize", handleScrollOrResize);
    window.addEventListener("scroll", handleScrollOrResize, true); // capture phase to catch all scrolls
    
    return () => {
      document.removeEventListener("mousedown", handleClickOutside);
      window.removeEventListener("resize", handleScrollOrResize);
      window.removeEventListener("scroll", handleScrollOrResize, true);
    };
  }, [isOpen]);

  const toggleDropdown = () => {
    if (disabled) return;
    if (!isOpen && wrapperRef.current) {
      const rect = wrapperRef.current.getBoundingClientRect();
      setDropdownStyles({
        position: 'absolute',
        top: rect.bottom + window.scrollY + 4,
        left: rect.left + window.scrollX,
        width: rect.width,
        zIndex: 99999, // extremely high to escape everything
      });
      setIsOpen(true);
    } else {
      setIsOpen(false);
      setQuery('');
    }
  };



  const filteredOptions = options.filter(opt =>
    opt.label.toLowerCase().includes(query.toLowerCase())
  );

  const selectedOpt = options.find(opt => String(opt.value) === String(value));
  const hasMore = !query && filteredOptions.length > 6;

  return (
    <div ref={wrapperRef} className="relative w-full">
      <div
        onClick={toggleDropdown}
        className={`w-full flex items-center justify-between rounded-lg border bg-white px-3 py-2 text-sm transition-all ${
          disabled
            ? 'opacity-50 cursor-not-allowed border-slate-200'
            : isOpen
              ? 'border-blue-400 ring-2 ring-blue-100 cursor-pointer'
              : 'border-slate-200 cursor-pointer hover:border-blue-400'
        }`}
      >
        <span className={selectedOpt ? 'text-slate-900' : 'text-slate-400'}>
          {selectedOpt ? selectedOpt.label : (disabled ? 'Loading...' : placeholder)}
        </span>
        <HiChevronDown className={`h-4 w-4 text-slate-400 transition-transform duration-200 ${isOpen ? 'rotate-180' : ''}`} />
      </div>

      {isOpen && createPortal(
        <div 
          id="searchable-select-portal"
          style={dropdownStyles}
          className="absolute overflow-hidden rounded-lg border border-slate-200 bg-white shadow-xl"
        >
          {/* Search box */}
          <div className="flex items-center gap-2 border-b border-slate-100 px-3 py-2.5 bg-slate-50">
            <HiMagnifyingGlass className="h-4 w-4 text-slate-400 flex-shrink-0" />
            <input
              type="text"
              autoFocus
              placeholder="Search..."
              value={query}
              onChange={(e) => setQuery(e.target.value)}
              className="w-full bg-transparent text-sm text-slate-900 placeholder-slate-400 focus:outline-none"
            />
            {query && (
              <button
                onClick={() => setQuery('')}
                className="text-slate-400 hover:text-slate-600 text-xs flex-shrink-0"
              >
                ✕
              </button>
            )}
          </div>

          {/* Count label */}
          {filteredOptions.length > 0 && (
            <div className="px-3 py-1 text-[11px] text-slate-400 border-b border-slate-50 bg-white">
              {query
                ? `${filteredOptions.length} result${filteredOptions.length !== 1 ? 's' : ''} found`
                : `${filteredOptions.length} option${filteredOptions.length !== 1 ? 's' : ''} — scroll to see all`
              }
            </div>
          )}

          {/* List */}
          <div ref={listRef} className="max-h-72 overflow-y-auto overscroll-contain">
            {filteredOptions.length === 0 ? (
              <div className="px-3 py-4 text-center text-sm text-slate-400">
                No results found
              </div>
            ) : (
              filteredOptions.map((opt) => {
                const isSelected = String(opt.value) === String(value);
                return (
                  <div
                    key={opt.value}
                    data-selected={isSelected}
                    onClick={() => {
                      onChange(opt.value);
                      setIsOpen(false);
                      setQuery('');
                    }}
                    className={`cursor-pointer px-3 py-2.5 text-sm transition-colors hover:bg-blue-50 hover:text-blue-700 ${
                      isSelected
                        ? 'bg-blue-50 font-semibold text-blue-700 border-l-2 border-blue-500'
                        : 'text-slate-700'
                    }`}
                  >
                    {opt.label}
                  </div>
                );
              })
            )}
          </div>

          {/* Scroll hint if many items */}
          {hasMore && (
            <div className="border-t border-slate-100 px-3 py-1.5 text-center text-[10px] text-slate-400 bg-slate-50">
              ↕ Scroll or type to search
            </div>
          )}
        </div>,
        document.body
      )}
    </div>
  );
}
