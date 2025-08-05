#!/usr/bin/env python3
import json
import re
import sys

def fix_empty_data_types(file_path):
    """Fix empty data types in grok patterns"""
    
    with open(file_path, 'r', encoding='utf-8') as f:
        data = json.load(f)
    
    # Find and fix empty data types
    fixed_count = 0
    for item in data:
        original_grok = item.get('grok_exp', '')
        # Replace %{PATTERN:FIELD:} with %{PATTERN:FIELD}
        fixed_grok = re.sub(r'%\{([^:}]+:[^:}]*?):\}', r'%{\1}', original_grok)
        # Also replace %{PATTERN:} with %{PATTERN}
        fixed_grok = re.sub(r'%\{([^:}]+):\}', r'%{\1}', fixed_grok)
        
        if original_grok != fixed_grok:
            fixed_count += 1
            print(f"Format ID: {item['format_id']}")
            # Find all patterns that will be fixed
            matches1 = re.findall(r'%\{([^:}]+:[^:}]*?):\}', original_grok)
            matches2 = re.findall(r'%\{([^:}]+):\}', original_grok)
            if matches1:
                print(f"Double colon patterns found: {matches1}")
            if matches2:
                print(f"Empty type patterns found: {matches2}")
            item['grok_exp'] = fixed_grok
    
    print(f"\nTotal fixed: {fixed_count}")
    
    # Save the fixed file
    with open(file_path, 'w', encoding='utf-8') as f:
        json.dump(data, f, ensure_ascii=False, indent=2)

if __name__ == "__main__":
    file_path = sys.argv[1] if len(sys.argv) > 1 else 'setting_logformat.json'
    fix_empty_data_types(file_path)