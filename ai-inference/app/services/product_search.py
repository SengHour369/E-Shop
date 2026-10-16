"""Conservative bilingual extraction for the registered read-only catalog tool."""

from __future__ import annotations

import re
from decimal import Decimal, InvalidOperation

_DIGITS = str.maketrans("០១២៣៤៥៦៧៨៩", "0123456789")
# Category vocabulary, not catalog records. Product IDs/prices always come from Catalog.
_CATEGORIES = {
    "ទូរស័ព្ទ": "phone", "ស្មាតហ្វូន": "smartphone",
    "ស្បែកជើង": "shoe", "កុំព្យូទ័រយួរដៃ": "laptop",
    "phones": "phone", "smartphones": "smartphone", "shoes": "shoe", "laptops": "laptop",
}


def extract_product_search(message: str) -> dict | None:
    text = message.translate(_DIGITS).replace("\u200b", "").strip().rstrip(".!?។")
    match = re.fullmatch(r"(?:find|show|search(?: for)?|look for)\s+(.+)", text, re.I)
    if not match:
        match = re.fullmatch(r"(?:ស្វែងរក|បង្ហាញ|រក)\s*(.+)", text)
    if not match:
        return None
    query = match.group(1).strip()
    filters: dict = {}
    price = re.search(
        r"\s*(?:priced?\s+)?(under|below|up to|over|above|តម្លៃក្រោម|តម្លៃលើស)\s*"
        r"\$?([0-9]+(?:\.[0-9]{1,2})?)\s*(?:dollars?|usd|ដុល្លារ)?$", query, re.I,
    )
    if price:
        try:
            amount = Decimal(price.group(2))
            if amount > 1_000_000:
                return None
        except InvalidOperation:
            return None
        key = "minPrice" if price.group(1).lower() in {"over", "above", "តម្លៃលើស"} else "maxPrice"
        filters[key] = float(amount)
        query = query[:price.start()].strip()
    if re.search(r"\bin stock\b|មានស្តុក", query, re.I):
        filters["inStock"] = True
        query = re.sub(r"\bin stock\b|មានស្តុក", "", query, flags=re.I).strip()
        query = re.sub(r"\s+", " ", query)
    if re.fullmatch(r"(?:product|sku)\s+[0-9]+", query, re.I):
        return None
    for source, target in _CATEGORIES.items():
        query = re.sub(r"(?<!\w)" + re.escape(source) + r"(?!\w)", target, query, flags=re.I)
    # Anything more complex is left to the structured provider, then server validation.
    if not query or len(query) > 120 or not re.fullmatch(r"[\w\s\-\u1780-\u17ff]+", query):
        return None
    if any(word in query.lower().split() for word in ("my", "order", "orders", "notifications", "ignore", "delete", "cancel")):
        return None
    return {"query": query, **filters}
