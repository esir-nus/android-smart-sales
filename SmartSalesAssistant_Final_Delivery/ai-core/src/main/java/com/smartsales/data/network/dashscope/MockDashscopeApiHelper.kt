package com.smartsales.data.network.dashscope

/**
 * Mock Dashscope API Helper for testing without actual SDK dependencies
 */
object MockDashscopeApiHelper {
    /**
     * Create customer analysis prompt
     */
    fun createCustomerAnalysisPrompt(transcription: String): String {
        return """
            请分析以下销售对话记录，提取关键的客户信息：
            
            对话记录：
            $transcription
            
            请按以下JSON格式返回分析结果：
            {
              "customer_name": "客户姓名",
              "company": "公司名称",
              "phone": "电话号码",
              "email": "邮箱",
              "wechat": "微信号",
              "position": "职位",
              "industry": "行业",
              "company_size": "公司规模",
              "location": "地点",
              "product_interest": "感兴趣的产品/服务",
              "budget_range": "预算范围",
              "decision_timeline": "决策时间线",
              "pain_points": ["痛点1", "痛点2"],
              "competitors": ["竞争对手1", "竞争对手2"],
              "decision_makers": ["决策者1", "决策者2"],
              "current_stage": "当前销售阶段",
              "next_follow_up": "下次跟进建议",
              "notes": "其他重要备注"
            }
            
            只返回JSON，不要添加其他说明文字。如果某些信息未提及，用null表示。
            """.trimIndent()
    }

    /**
     * Create conversation summary prompt
     */
    fun createSummaryPrompt(
        transcription: String,
        maxLength: Int = 200,
    ): String {
        return """
            请用不超过${maxLength}字总结以下销售对话的核心内容：
            
            对话记录：
            $transcription
            
            总结要点：
            1. 客户主要需求
            2. 讨论的关键问题
            3. 达成的初步共识
            4. 下一步行动计划
            
            请直接给出总结内容，不要添加"总结："等前缀。
            """.trimIndent()
    }

    /**
     * Create follow-up suggestion prompt
     */
    fun createFollowUpPrompt(
        transcription: String,
        customerInfo: String,
    ): String {
        return """
            基于以下销售对话和客户信息，建议下次跟进的具体行动：
            
            对话记录：
            $transcription
            
            客户信息：
            $customerInfo
            
            请提供：
            1. 建议的跟进时间（几天后）
            2. 跟进方式（电话/邮件/微信/拜访）
            3. 跟进的具体话题和目标
            4. 需要准备的材料或信息
            
            请以清晰的要点形式返回，每个要点一行。
            """.trimIndent()
    }

    /**
     * Create sales strategy prompt
     */
    fun createSalesStrategyPrompt(
        transcription: String,
        customerInfo: String,
    ): String {
        return """
            基于以下信息，制定针对性的销售策略：
            
            对话记录：
            $transcription
            
            客户信息：
            $customerInfo
            
            请分析并提供：
            1. 客户的核心痛点和需求
            2. 我们的优势和价值主张
            3. 潜在的异议和应对策略
            4. 推进成交的关键步骤
            5. 风险评估和预防措施
            
            请以专业的销售顾问角度提供建议。
            """.trimIndent()
    }
}
