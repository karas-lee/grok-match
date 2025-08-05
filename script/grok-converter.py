#!/usr/bin/env python3
"""
LOGCENTER-LOG-FORMAT.sql에서 data_table 항목을 제거하여 
GROK-PATTERN-CONVERTER.sql 파일로 변환하는 스크립트
"""

import json
import sys
import re


def remove_data_table(data):
    """
    재귀적으로 JSON 구조를 탐색하며 data_table 키를 제거
    """
    if isinstance(data, dict):
        # data_table 키가 있다면 제거
        if 'data_table' in data:
            del data['data_table']
        
        # 나머지 키들에 대해 재귀적으로 처리
        for key, value in data.items():
            remove_data_table(value)
    
    elif isinstance(data, list):
        # 리스트의 각 요소에 대해 재귀적으로 처리
        for item in data:
            remove_data_table(item)
    
    return data


def process_json_manually(input_file, output_file):
    """
    파일을 줄 단위로 처리하여 data_table 섹션을 제거
    """
    with open(input_file, 'r', encoding='utf-8') as infile:
        lines = infile.readlines()
    
    output_lines = []
    in_data_table = False
    brace_count = 0
    skip_next_comma = False
    
    for i, line in enumerate(lines):
        stripped = line.strip()
        
        # data_table 시작 감지
        if '"data_table"' in line and ':' in line:
            in_data_table = True
            brace_count = 0
            # data_table이 배열인지 확인
            if '[' in line:
                brace_count = 1
            continue
        
        # data_table 내부에서 중괄호/대괄호 카운트
        if in_data_table:
            brace_count += line.count('[') + line.count('{')
            brace_count -= line.count(']') + line.count('}')
            
            # data_table 종료
            if brace_count == 0:
                in_data_table = False
                skip_next_comma = True
            continue
        
        # data_table 다음의 콤마 제거
        if skip_next_comma and stripped.startswith(','):
            line = line.replace(',', '', 1)
            skip_next_comma = False
        elif skip_next_comma:
            skip_next_comma = False
        
        output_lines.append(line)
    
    # 결과 저장
    with open(output_file, 'w', encoding='utf-8') as outfile:
        outfile.writelines(output_lines)


def main():
    try:
        # 입력/출력 파일 경로
        input_file = 'LOGCENTER-LOG-FORMAT.sql'
        output_file = 'GROK-PATTERN-CONVERTER.sql'
        
        print(f"Processing {input_file}...")
        print("Removing data_table entries...")
        
        # 수동으로 JSON 처리
        process_json_manually(input_file, output_file)
        
        print(f"✓ Successfully converted {input_file} to {output_file}")
        print(f"  All 'data_table' entries have been removed.")
        
        # 변환된 파일이 유효한 JSON인지 검증
        print("\nValidating output JSON...")
        try:
            with open(output_file, 'r', encoding='utf-8') as f:
                json.load(f)
            print("✓ Output file is valid JSON")
        except json.JSONDecodeError as e:
            print(f"Warning: Output file may have JSON syntax issues - {e}")
            print("You may need to manually fix the file or use a JSON validator.")
        
    except FileNotFoundError:
        print(f"Error: {input_file} not found.")
        sys.exit(1)
    except Exception as e:
        print(f"Error: {e}")
        sys.exit(1)


if __name__ == "__main__":
    main()