/**
 * 数据访问层包
 * 包含JPA Repository接口，提供数据持久化操作
 * 
 * 主要Repository接口：
 * - UserRepository: 用户数据访问接口，提供用户管理相关查询
 * - GestureAnalysisRecordRepository: 手势分析记录数据访问接口，提供分析记录的CRUD和统计查询
 * - ExperimentSessionRepository: 实验会话数据访问接口，提供会话管理和监控查询
 * - ApiCallLogRepository: API调用日志数据访问接口，提供API监控和统计查询
 * 
 * 所有Repository接口都继承自JpaRepository，提供标准的CRUD操作，
 * 同时包含针对业务需求的自定义查询方法和统计分析功能。
 */
package com.education.ai.repository;