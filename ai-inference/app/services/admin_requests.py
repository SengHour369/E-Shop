"""Exact bilingual read requests. No inferred identifiers and no writes."""
from __future__ import annotations
import re
from app.schemas.tool_definition import ToolDefinition

_READS = {
    'ADMIN_ORDER_LIST': ('show orders', 'list orders', 'បង្ហាញការបញ្ជាទិញ', 'មើលការបញ្ជាទិញ'),
    'ADMIN_PAYMENT_LIST': ('show payments', 'list payments', 'បង្ហាញការទូទាត់', 'មើលការទូទាត់'),
    'ADMIN_RETURN_LIST': ('show returns', 'list returns', 'បង្ហាញការត្រឡប់ទំនិញ'),
    'ADMIN_USER_LIST': ('show users', 'list users', 'show customers', 'បង្ហាញអ្នកប្រើប្រាស់', 'បង្ហាញអតិថិជន'),
    'ADMIN_ORDER_SUMMARY': ('show order counts', 'order summary', 'សង្ខេបការបញ្ជាទិញ'),
    'INVENTORY_LOW_STOCK': ('show low stock', 'list low stock products', 'បង្ហាញផលិតផលជិតអស់ស្តុក', 'មើលស្តុកតិច'),
    'ADMIN_TOOL_LIST': ('show available tools', 'list available service apis', 'បង្ហាញមុខងារដែលអាចប្រើបាន'),
    'ADMIN_SERVICE_STATUS': ('show admin service status', 'admin service status'),
}

def extract_admin_read(message: str, tools: list[ToolDefinition]) -> tuple[str, dict] | None:
    # Appended context is not part of the current command. It never supplies IDs here.
    command = message.split('\nPrevious backend result references', 1)[0]
    command = re.sub(r'\s+', ' ', command.replace('\u200b', '').strip().lower()).rstrip('.!?។៖')
    command = re.sub(r'^(?:please |សូម\s*)', '', command)
    catalog = {tool.name: tool for tool in tools}
    for name, phrases in _READS.items():
        tool = catalog.get(name)
        if tool is None:
            continue
        for phrase in phrases:
            params = {}
            if command == phrase:
                return name, params
            suffix = command[len(phrase):].strip() if command.startswith(phrase) else None
            if suffix in ('today', 'for today', 'ថ្ងៃនេះ') and 'date' in tool.parameters:
                return name, {'date': 'TODAY'}
            if suffix and 'status' in tool.parameters:
                status = re.fullmatch(r'(?:with status |status )?(pending|paid|processing|confirmed|shipped|delivered|cancelled|failed|refunded)', suffix)
                if status:
                    return name, {'status': status.group(1).upper()}
    return None
