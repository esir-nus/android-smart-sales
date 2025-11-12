package com.smartsales.data.network.dashscope

import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.flow
import javax.inject.Inject
import javax.inject.Singleton

/**
 * Mock implementation of DashscopeChatClient for testing without actual SDK dependencies
 */
@Singleton
class MockDashscopeChatClient
    @Inject
    constructor() : DashscopeChatClient {
        override fun streamChat(payload: DashscopeChatPayload): Flow<DashscopeStreamEvent> =
            flow {
                // Mock implementation - simulate streaming response
                val mockResponse =
                    when {
                        payload.input.messages.any { it.content.contains("总结") } -> {
                            """
                            对话摘要：
                            1. 客户主要需求：了解产品功能和价格
                            2. 关键问题：预算限制和实施方案  
                            3. 初步共识：产品符合需求
                            4. 下一步行动：提供详细报价和演示
                            """.trimIndent()
                        }
                        payload.input.messages.any { it.content.contains("客户") || it.content.contains("分析") } -> {
                            """
                            {
                              "customer_name": "张先生",
                              "company": "科技有限公司", 
                              "phone": "138****8888",
                              "product_interest": "智能销售系统",
                              "budget_range": "10-50万",
                              "pain_points": ["销售效率低", "客户管理困难"],
                              "next_follow_up": "3天后电话跟进"
                            }
                            """.trimIndent()
                        }
                        payload.input.messages.any {
                            it.content.contains(
                                "思维导图",
                            ) || it.content.contains("mindmap")
                        } -> {
                            """
                            销售对话思维导图：
                            
                            中心主题：智能销售系统咨询
                            
                            ├─ 客户需求
                            │  ├─ 功能需求
                            │  ├─ 价格预算  
                            │  └─ 实施时间
                            
                            ├─ 产品亮点
                            │  ├─ AI智能分析
                            │  ├─ 客户管理
                            │  └─ 销售自动化
                            
                            ├─ 异议处理
                            │  ├─ 价格异议
                            │  ├─ 功能疑虑
                            │  └─ 竞品比较
                            
                            └─ 行动计划
                               ├─ 提供报价
                               ├─ 安排演示
                               └─ 跟进签约
                            """.trimIndent()
                        }
                        else -> {
                            """
                            这是模拟的Dashscope AI助手响应。我可以帮助您：
                            
                            1. 分析销售对话内容
                            2. 提取关键客户信息  
                            3. 生成对话摘要
                            4. 创建思维导图
                            5. 提供销售建议
                            
                            请告诉我您需要哪种类型的分析？
                            """.trimIndent()
                        }
                    }

                // Simulate streaming by sending chunks
                val chunks = mockResponse.split("")
                for (chunk in chunks) {
                    if (chunk.isNotEmpty()) {
                        emit(DashscopeStreamEvent.Chunk(chunk))
                        kotlinx.coroutines.delay(50) // Simulate streaming delay
                    }
                }

                emit(
                    DashscopeStreamEvent.Completed(
                        usageTokens = mockResponse.length,
                        requestId = "mock-request-${System.currentTimeMillis()}",
                    ),
                )
            }
    }
