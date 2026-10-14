// src/utils/formatPhone.js
// Phone number formatter utility.

/**
 * Format a phone number string.
 * @param {string} phone
 * @returns {string}
 */
export function formatPhone(phone) {
  if (!phone) return '';
  // Remove all non-digit characters for processing
  const digits = phone.replace(/\D/g, '');
  // Simple US format: (555) 000-0000
  if (digits.length === 10) {
    return `(${digits.slice(0, 3)}) ${digits.slice(3, 6)}-${digits.slice(6)}`;
  }
  // International or already-formatted: return as-is
  return phone;
}

export default formatPhone;
