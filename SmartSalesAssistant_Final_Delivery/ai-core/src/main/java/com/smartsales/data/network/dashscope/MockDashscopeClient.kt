package com.smartsales.data.network.dashscope

import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.flow

/**
 * Mock Dashscope client for testing without actual Alibaba SDK dependencies
 */
class MockDashscopeStreamingSdk {
    fun streamGeneration(prompt: String): Flow<String> =
        flow {
            // Mock streaming response
            val responses =
                listOf(
                    "这是",
                    "一个",
                    "测试",
                    "回答。",
                    "",
                    "模拟",
                    "Dashscope",
                    "流式",
                    "响应。",
                )

            for (response in responses) {
                emit(response)
                kotlinx.coroutines.delay(100) // Simulate streaming delay
            }
        }

    fun generateCompletion(prompt: String): String {
        // Mock completion response
        return when {
            prompt.contains("总结") ->
                """
                对话摘要：
                1. 客户主要需求：了解产品功能和价格
                2. 关键问题：预算限制和实施方案
                3. 初步共识：产品符合需求
                4. 下一步行动：提供详细报价和演示
                """.trimIndent()

            prompt.contains("客户") ->
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

            prompt.contains("思维导图") ->
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

            else -> "这是模拟的Dashscope响应内容。"
        }
    }
}
