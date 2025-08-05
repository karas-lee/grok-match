#!/usr/bin/env python3
"""
GROK-PATTERN-CONVERTER.sql 파일의 잘못된 JSON 이스케이프를 수정하는 스크립트
Grok 패턴에 있는 백슬래시를 올바른 JSON 형식으로 변환
"""

import json
import re
import sys
from pathlib import Path

def fix_json_string(s):
    """
    JSON 문자열 내의 이스케이프 시퀀스를 수정
    """
    # 이미 올바른 이스케이프를 보호
    s = s.replace('\\\\', '\x00')  # 임시로 null 문자로 치환
    s = s.replace('\\"', '\x01')   # 임시로 SOH 문자로 치환
    
    # 나머지 백슬래시를 이중 백슬래시로
    s = s.replace('\\', '\\\\')
    
    # 보호했던 것들을 복원
    s = s.replace('\x00', '\\\\')
    s = s.replace('\x01', '\\"')
    
    return s

def fix_json_file(input_file, output_file):
    """
    JSON 파일의 잘못된 이스케이프 시퀀스를 수정
    """
    print(f"입력 파일: {input_file}")
    print(f"출력 파일: {output_file}")
    
    try:
        # 파일 읽기
        with open(input_file, 'r', encoding='utf-8') as f:
            lines = f.readlines()
        
        # 각 라인을 처리
        fixed_lines = []
        in_string = False
        current_field = None
        
        for line_num, line in enumerate(lines):
            # grok_exp나 samplelog 필드를 찾기
            if '"grok_exp"' in line or '"samplelog"' in line:
                # 필드명과 값을 분리
                match = re.match(r'(\s*"(grok_exp|samplelog)"\s*:\s*)"(.*)",?\s*$', line)
                if match:
                    prefix = match.group(1)
                    field_name = match.group(2)
                    value = match.group(3)
                    
                    # 값 수정
                    fixed_value = fix_json_string(value)
                    
                    # 라인 재구성
                    if line.rstrip().endswith(','):
                        fixed_line = f'{prefix}"{fixed_value}",\n'
                    else:
                        fixed_line = f'{prefix}"{fixed_value}"\n'
                    
                    fixed_lines.append(fixed_line)
                else:
                    fixed_lines.append(line)
            else:
                fixed_lines.append(line)
        
        # 수정된 내용을 문자열로 합치기
        content = ''.join(fixed_lines)
        
        # JSON 파싱 테스트
        try:
            data = json.loads(content)
            print(f"JSON 파싱 성공! {len(data)}개의 포맷을 찾았습니다.")
            
            # 파일 저장
            with open(output_file, 'w', encoding='utf-8') as f:
                json.dump(data, f, ensure_ascii=False, indent=2)
            
            print("파일 변환 완료!")
            
            # 통계 출력
            total_patterns = 0
            for format_data in data:
                if 'log_type' in format_data:
                    for log_type in format_data['log_type']:
                        if 'patterns' in log_type:
                            total_patterns += len(log_type['patterns'])
            
            print(f"총 {len(data)}개의 포맷, {total_patterns}개의 패턴")
            
        except json.JSONDecodeError as e:
            print(f"JSON 파싱 오류: {e}")
            # 오류 위치 출력
            error_line = e.lineno - 1
            if 0 <= error_line < len(lines):
                print(f"\n오류 라인 {e.lineno}:")
                print(lines[error_line][:200])
            
            # 디버그용 임시 파일 저장
            debug_file = output_file.with_suffix('.debug.json')
            with open(debug_file, 'w', encoding='utf-8') as f:
                f.write(content)
            print(f"\n디버그용 파일 저장: {debug_file}")
    
    except Exception as e:
        print(f"오류 발생: {e}")
        import traceback
        traceback.print_exc()
        sys.exit(1)

if __name__ == "__main__":
    # 프로젝트 루트 경로 찾기
    script_dir = Path(__file__).parent
    project_root = script_dir.parent
    
    # 입력 및 출력 파일 경로
    input_file = project_root / "src/main/resources/GROK-PATTERN-CONVERTER.sql"
    output_file = project_root / "src/main/resources/GROK-PATTERN-CONVERTER-FIXED.json"
    
    # 변환 실행
    fix_json_file(input_file, output_file)