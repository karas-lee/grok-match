package com.logcenter.recommender.grok;

import com.logcenter.recommender.model.LogFormat;
import java.util.List;
import java.util.Map;

/**
 * 로그 포맷 패턴 저장소 인터페이스
 * GROK-PATTERN-CONVERTER.sql 파일의 데이터를 관리
 */
public interface PatternRepository {
    
    /**
     * 패턴 저장소 초기화
     * @return 초기화 성공 여부
     */
    boolean initialize();
    
    /**
     * 모든 로그 포맷 로드
     * @return 로드된 포맷 개수
     */
    int loadFormats();
    
    /**
     * 모든 로그 포맷 가져오기
     * @return 로그 포맷 리스트
     */
    List<LogFormat> getAllFormats();
    
    /**
     * ID로 로그 포맷 검색
     * @param formatId 포맷 ID
     * @return LogFormat 객체, 없으면 null
     */
    LogFormat getFormatById(String formatId);
    
    /**
     * 그룹별로 로그 포맷 검색
     * @param groupName 그룹 이름 (예: Firewall, Web Server)
     * @return 해당 그룹의 로그 포맷 리스트
     */
    List<LogFormat> getFormatsByGroup(String groupName);
    
    /**
     * 벤더별로 로그 포맷 검색
     * @param vendor 벤더명
     * @return 해당 벤더의 로그 포맷 리스트
     */
    List<LogFormat> getFormatsByVendor(String vendor);
    
    /**
     * 그룹별 포맷 개수 통계
     * @return 그룹별 포맷 개수 맵
     */
    Map<String, Integer> getGroupStatistics();
    
    /**
     * 벤더별 포맷 개수 통계
     * @return 벤더별 포맷 개수 맵
     */
    Map<String, Integer> getVendorStatistics();
    
    /**
     * 포맷 재로드
     * @return 재로드된 포맷 개수
     */
    int reloadFormats();
    
    /**
     * 저장소 크기 반환
     * @return 저장된 포맷 개수
     */
    int size();
    
    /**
     * 저장소 비우기
     */
    void clear();
}