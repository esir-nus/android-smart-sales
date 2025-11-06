# 阿里云听悟 (Tingwu) API 官方示例代码

**版本**: Tingwu v2 API (2023-09-30)  
**来源**: 阿里云官方文档  
**更新日期**: 2025-10-28  
**状态**: 生产环境可用

---

## 📑 目录 (Table of Contents)

1. [概述](#概述)
2. [环境配置](#环境配置)
3. [基础示例](#基础示例)
   - [1. 语音转写（含角色分离）](#1-语音转写含角色分离)
   - [2. 文本翻译](#2-文本翻译)
   - [3. 章节速览](#3-章节速览)
   - [4. 摘要总结](#4-摘要总结)
   - [5. 要点提炼](#5-要点提炼)
   - [6. PPT抽取摘要](#6-ppt抽取摘要)
   - [7. 口语书面化](#7-口语书面化)
4. [高级功能](#高级功能)
   - [8. 身份识别](#8-身份识别)
   - [9. 对话内容提取](#9-对话内容提取)
   - [10. 服务质检](#10-服务质检)
   - [11. 自定义Prompt](#11-自定义prompt)
5. [公共方法说明](#公共方法说明)
6. [API参数说明](#api参数说明)

---

## 概述

本文档提供阿里云听悟 (Tingwu) API v2 的官方示例代码，涵盖音频转写、翻译、摘要、质检等核心功能。所有示例均经过官方验证，可直接用于生产环境。

### 核心特性

- ✅ **语音转写**: 支持角色分离，识别多个说话人
- ✅ **智能翻译**: 支持多语言实时翻译
- ✅ **内容分析**: 章节速览、摘要总结、要点提炼
- ✅ **质量检测**: 身份识别、对话提取、服务质检
- ✅ **自定义能力**: 支持自定义Prompt进行个性化分析

### API端点信息

- **域名**: `tingwu.cn-beijing.aliyuncs.com`
- **版本**: `2023-09-30`
- **协议**: HTTPS
- **方法**: PUT (提交任务), GET (查询结果)
- **路径**: `/openapi/tingwu/v2/tasks`

---

## 环境配置

### 必需依赖

```bash
pip install aliyun-python-sdk-core>=2.13.0
```

### 环境变量设置

在运行示例代码前，请设置以下环境变量：

```bash
# Linux/Mac
export ALIBABA_CLOUD_ACCESS_KEY_ID="your-access-key-id"
export ALIBABA_CLOUD_ACCESS_KEY_SECRET="your-access-key-secret"

# Windows PowerShell
$env:ALIBABA_CLOUD_ACCESS_KEY_ID="your-access-key-id"
$env:ALIBABA_CLOUD_ACCESS_KEY_SECRET="your-access-key-secret"
```

### 获取凭证

1. **AppKey**: 登录 [听悟控制台](https://tingwu.aliyun.com/console) 创建应用获取
2. **AccessKey**: 登录 [RAM控制台](https://ram.console.aliyun.com/manage/ak) 获取

---

## 基础示例

### 1. 语音转写（含角色分离）

**功能说明**: 将音频文件转写为文字，并识别说话人角色（如销售、客户）

**适用场景**: 会议记录、客服对话、销售录音分析

**参数配置**:
- `DiarizationEnabled: True` - 启用角色分离
- `SpeakerCount: 2` - 设置说话人数量

```python
#!/usr/bin/env python
# coding=utf-8

import os
import json
import datetime
from aliyunsdkcore.client import AcsClient
from aliyunsdkcore.request import CommonRequest
from aliyunsdkcore.auth.credentials import AccessKeyCredential

def create_common_request(domain, version, protocolType, method, uri):
    """创建通用请求对象"""
    request = CommonRequest()
    request.set_accept_format('json')
    request.set_domain(domain)
    request.set_version(version)
    request.set_protocol_type(protocolType)
    request.set_method(method)
    request.set_uri_pattern(uri)
    request.add_header('Content-Type', 'application/json')
    return request

def init_parameters():
    """初始化请求参数"""
    root = dict()
    root['AppKey'] = '输入您在听悟管控台创建的Appkey'
    
    # 基本请求参数
    input = dict()
    input['SourceLanguage'] = 'cn'
    input['TaskKey'] = 'task' + datetime.datetime.now().strftime('%Y%m%d%H%M%S')
    input['FileUrl'] = '输入待测试的音频url链接'
    root['Input'] = input
    
    # AI相关参数
    parameters = dict()
    
    # 语音识别控制 - 角色分离
    transcription = dict()
    transcription['DiarizationEnabled'] = True  # 启用角色分离
    diarization = dict()
    diarization['SpeakerCount'] = 2  # 说话人数量
    transcription['Diarization'] = diarization
    parameters['Transcription'] = transcription
    
    root['Parameters'] = parameters
    return root

# 执行流程
body = init_parameters()
print(body)

# 通过环境变量设置AccessKey
credentials = AccessKeyCredential(
    os.environ['ALIBABA_CLOUD_ACCESS_KEY_ID'], 
    os.environ['ALIBABA_CLOUD_ACCESS_KEY_SECRET']
)
client = AcsClient(region_id='cn-beijing', credential=credentials)

# 提交任务
request = create_common_request(
    'tingwu.cn-beijing.aliyuncs.com', 
    '2023-09-30', 
    'https', 
    'PUT', 
    '/openapi/tingwu/v2/tasks'
)
request.add_query_param('type', 'offline')
request.set_content(json.dumps(body).encode('utf-8'))

response = client.do_action_with_exception(request)
print("response: \n" + json.dumps(json.loads(response), indent=4, ensure_ascii=False))
```

---

### 2. 文本翻译

**功能说明**: 在转写的同时将内容翻译为目标语言

**适用场景**: 跨国会议、多语言客服、国际商务沟通

**参数配置**:
- `TranslationEnabled: True` - 启用翻译
- `TargetLanguages: ['en']` - 目标语言列表

```python
#!/usr/bin/env python
# coding=utf-8

import os
import json
import datetime
from aliyunsdkcore.client import AcsClient
from aliyunsdkcore.request import CommonRequest
from aliyunsdkcore.auth.credentials import AccessKeyCredential

def create_common_request(domain, version, protocolType, method, uri):
    """创建通用请求对象"""
    request = CommonRequest()
    request.set_accept_format('json')
    request.set_domain(domain)
    request.set_version(version)
    request.set_protocol_type(protocolType)
    request.set_method(method)
    request.set_uri_pattern(uri)
    request.add_header('Content-Type', 'application/json')
    return request

def init_parameters():
    """初始化请求参数"""
    root = dict()
    root['AppKey'] = '输入您在听悟管控台创建的Appkey'
    
    # 基本请求参数
    input = dict()
    input['SourceLanguage'] = 'cn'
    input['TaskKey'] = 'task' + datetime.datetime.now().strftime('%Y%m%d%H%M%S')
    input['FileUrl'] = '输入待测试的音频url链接'
    root['Input'] = input
    
    # AI相关参数
    parameters = dict()
    
    # 文本翻译控制
    parameters['TranslationEnabled'] = True
    translation = dict()
    translation['TargetLanguages'] = ['en']  # 翻译成英文
    parameters['Translation'] = translation
    
    root['Parameters'] = parameters
    return root

# 执行流程
body = init_parameters()
print(body)

credentials = AccessKeyCredential(
    os.environ['ALIBABA_CLOUD_ACCESS_KEY_ID'], 
    os.environ['ALIBABA_CLOUD_ACCESS_KEY_SECRET']
)
client = AcsClient(region_id='cn-beijing', credential=credentials)

request = create_common_request(
    'tingwu.cn-beijing.aliyuncs.com', 
    '2023-09-30', 
    'https', 
    'PUT', 
    '/openapi/tingwu/v2/tasks'
)
request.add_query_param('type', 'offline')
request.set_content(json.dumps(body).encode('utf-8'))

response = client.do_action_with_exception(request)
print("response: \n" + json.dumps(json.loads(response), indent=4, ensure_ascii=False))
```

---

### 3. 章节速览

**功能说明**: 自动将长音频分割为章节，生成章节标题和时间戳

**适用场景**: 会议记录、课程录音、播客整理

**参数配置**:
- `AutoChaptersEnabled: True` - 启用章节速览
- `ChapterGranularity: "Coarse"` - 章节粒度（粗/细）
- `TitleLengthLevel: "Short"` - 标题长度（短/中/长）

```python
#!/usr/bin/env python
# coding=utf-8

import os
import json
import datetime
from aliyunsdkcore.client import AcsClient
from aliyunsdkcore.request import CommonRequest
from aliyunsdkcore.auth.credentials import AccessKeyCredential

def create_common_request(domain, version, protocolType, method, uri):
    """创建通用请求对象"""
    request = CommonRequest()
    request.set_accept_format('json')
    request.set_domain(domain)
    request.set_version(version)
    request.set_protocol_type(protocolType)
    request.set_method(method)
    request.set_uri_pattern(uri)
    request.add_header('Content-Type', 'application/json')
    return request

def init_parameters():
    """初始化请求参数"""
    root = dict()
    root['AppKey'] = '输入您在听悟管控台创建的Appkey'
    
    # 基本请求参数
    input = dict()
    input['SourceLanguage'] = 'cn'
    input['TaskKey'] = 'task' + datetime.datetime.now().strftime('%Y%m%d%H%M%S')
    input['FileUrl'] = '输入待测试的音频url链接'
    root['Input'] = input
    
    # AI相关参数
    parameters = dict()
    
    # 章节速览
    parameters['AutoChaptersEnabled'] = True
    parameters['AutoChapters'] = {
        "ChapterGranularity": "Coarse",  # 粗粒度章节
        "TitleLengthLevel": "Short"       # 短标题
    }
    
    root['Parameters'] = parameters
    return root

# 执行流程
body = init_parameters()
print(body)

credentials = AccessKeyCredential(
    os.environ['ALIBABA_CLOUD_ACCESS_KEY_ID'], 
    os.environ['ALIBABA_CLOUD_ACCESS_KEY_SECRET']
)
client = AcsClient(region_id='cn-beijing', credential=credentials)

request = create_common_request(
    'tingwu.cn-beijing.aliyuncs.com', 
    '2023-09-30', 
    'https', 
    'PUT', 
    '/openapi/tingwu/v2/tasks'
)
request.add_query_param('type', 'offline')
request.set_content(json.dumps(body).encode('utf-8'))

response = client.do_action_with_exception(request)
print("response: \n" + json.dumps(json.loads(response), indent=4, ensure_ascii=False))
```

---

### 4. 摘要总结

**功能说明**: 生成音频内容的摘要、总结、问答回顾、思维导图等

**适用场景**: 快速了解会议要点、课程总结、播客精华提取

**参数配置**:
- `PptExtractionEnabled: True` - 启用PPT抽取和总结

```python
#!/usr/bin/env python
# coding=utf-8

import os
import json
import datetime
from aliyunsdkcore.client import AcsClient
from aliyunsdkcore.request import CommonRequest
from aliyunsdkcore.auth.credentials import AccessKeyCredential

def create_common_request(domain, version, protocolType, method, uri):
    """创建通用请求对象"""
    request = CommonRequest()
    request.set_accept_format('json')
    request.set_domain(domain)
    request.set_version(version)
    request.set_protocol_type(protocolType)
    request.set_method(method)
    request.set_uri_pattern(uri)
    request.add_header('Content-Type', 'application/json')
    return request

def init_parameters():
    """初始化请求参数"""
    root = dict()
    root['AppKey'] = '输入您在听悟管控台创建的Appkey'
    
    # 基本请求参数
    input = dict()
    input['SourceLanguage'] = 'cn'
    input['TaskKey'] = 'task' + datetime.datetime.now().strftime('%Y%m%d%H%M%S')
    input['FileUrl'] = '输入待测试的音频url链接'
    root['Input'] = input
    
    # AI相关参数
    parameters = dict()
    
    # PPT抽取和PPT总结
    parameters['PptExtractionEnabled'] = True
    
    root['Parameters'] = parameters
    return root

# 执行流程
body = init_parameters()
print(body)

credentials = AccessKeyCredential(
    os.environ['ALIBABA_CLOUD_ACCESS_KEY_ID'], 
    os.environ['ALIBABA_CLOUD_ACCESS_KEY_SECRET']
)
client = AcsClient(region_id='cn-beijing', credential=credentials)

request = create_common_request(
    'tingwu.cn-beijing.aliyuncs.com', 
    '2023-09-30', 
    'https', 
    'PUT', 
    '/openapi/tingwu/v2/tasks'
)
request.add_query_param('type', 'offline')
request.set_content(json.dumps(body).encode('utf-8'))

response = client.do_action_with_exception(request)
print("response: \n" + json.dumps(json.loads(response), indent=4, ensure_ascii=False))
```

---

### 5. 要点提炼

**功能说明**: 提炼音频中的待办事项、关键词、重点内容

**适用场景**: 任务跟踪、行动项提取、关键决策记录

**参数配置**:
- `MeetingAssistanceEnabled: True` - 启用会议助手
- `Types: ['Actions', 'KeyInformation']` - 提取类型

```python
#!/usr/bin/env python
# coding=utf-8

import os
import json
import datetime
from aliyunsdkcore.client import AcsClient
from aliyunsdkcore.request import CommonRequest
from aliyunsdkcore.auth.credentials import AccessKeyCredential

def create_common_request(domain, version, protocolType, method, uri):
    """创建通用请求对象"""
    request = CommonRequest()
    request.set_accept_format('json')
    request.set_domain(domain)
    request.set_version(version)
    request.set_protocol_type(protocolType)
    request.set_method(method)
    request.set_uri_pattern(uri)
    request.add_header('Content-Type', 'application/json')
    return request

def init_parameters():
    """初始化请求参数"""
    root = dict()
    root['AppKey'] = '输入您在听悟管控台创建的Appkey'
    
    # 基本请求参数
    input = dict()
    input['SourceLanguage'] = 'cn'
    input['TaskKey'] = 'task' + datetime.datetime.now().strftime('%Y%m%d%H%M%S')
    input['FileUrl'] = '输入待测试的音频url链接'
    root['Input'] = input
    
    # AI相关参数
    parameters = dict()
    
    # 要点提炼
    parameters['MeetingAssistanceEnabled'] = True
    meetingAssistance = dict()
    meetingAssistance['Types'] = ['Actions', 'KeyInformation']  # 行动项和关键信息
    parameters['MeetingAssistance'] = meetingAssistance
    
    root['Parameters'] = parameters
    return root

# 执行流程
body = init_parameters()
print(body)

credentials = AccessKeyCredential(
    os.environ['ALIBABA_CLOUD_ACCESS_KEY_ID'], 
    os.environ['ALIBABA_CLOUD_ACCESS_KEY_SECRET']
)
client = AcsClient(region_id='cn-beijing', credential=credentials)

request = create_common_request(
    'tingwu.cn-beijing.aliyuncs.com', 
    '2023-09-30', 
    'https', 
    'PUT', 
    '/openapi/tingwu/v2/tasks'
)
request.add_query_param('type', 'offline')
request.set_content(json.dumps(body).encode('utf-8'))

response = client.do_action_with_exception(request)
print("response: \n" + json.dumps(json.loads(response), indent=4, ensure_ascii=False))
```

---

### 6. PPT抽取摘要

**功能说明**: 从演示文稿录音中抽取PPT内容并生成摘要

**适用场景**: 培训课程、产品发布会、学术讲座

**参数配置**:
- `PptExtractionEnabled: True` - 启用PPT抽取

```python
#!/usr/bin/env python
# coding=utf-8

import os
import json
import datetime
from aliyunsdkcore.client import AcsClient
from aliyunsdkcore.request import CommonRequest
from aliyunsdkcore.auth.credentials import AccessKeyCredential

def create_common_request(domain, version, protocolType, method, uri):
    """创建通用请求对象"""
    request = CommonRequest()
    request.set_accept_format('json')
    request.set_domain(domain)
    request.set_version(version)
    request.set_protocol_type(protocolType)
    request.set_method(method)
    request.set_uri_pattern(uri)
    request.add_header('Content-Type', 'application/json')
    return request

def init_parameters():
    """初始化请求参数"""
    root = dict()
    root['AppKey'] = '输入您在听悟管控台创建的Appkey'
    
    # 基本请求参数
    input = dict()
    input['SourceLanguage'] = 'cn'
    input['TaskKey'] = 'task' + datetime.datetime.now().strftime('%Y%m%d%H%M%S')
    input['FileUrl'] = '输入待测试的音频url链接'
    root['Input'] = input
    
    # AI相关参数
    parameters = dict()
    
    # PPT抽取和PPT总结
    parameters['PptExtractionEnabled'] = True
    
    root['Parameters'] = parameters
    return root

# 执行流程
body = init_parameters()
print(body)

credentials = AccessKeyCredential(
    os.environ['ALIBABA_CLOUD_ACCESS_KEY_ID'], 
    os.environ['ALIBABA_CLOUD_ACCESS_KEY_SECRET']
)
client = AcsClient(region_id='cn-beijing', credential=credentials)

request = create_common_request(
    'tingwu.cn-beijing.aliyuncs.com', 
    '2023-09-30', 
    'https', 
    'PUT', 
    '/openapi/tingwu/v2/tasks'
)
request.add_query_param('type', 'offline')
request.set_content(json.dumps(body).encode('utf-8'))

response = client.do_action_with_exception(request)
print("response: \n" + json.dumps(json.loads(response), indent=4, ensure_ascii=False))
```

---

### 7. 口语书面化

**功能说明**: 将口语化的转写结果转换为书面语，提高文本可读性

**适用场景**: 正式文档生成、会议纪要、报告撰写

**参数配置**:
- `TextPolishEnabled: True` - 启用文本润色

```python
#!/usr/bin/env python
# coding=utf-8

import os
import json
import datetime
from aliyunsdkcore.client import AcsClient
from aliyunsdkcore.request import CommonRequest
from aliyunsdkcore.auth.credentials import AccessKeyCredential

def create_common_request(domain, version, protocolType, method, uri):
    """创建通用请求对象"""
    request = CommonRequest()
    request.set_accept_format('json')
    request.set_domain(domain)
    request.set_version(version)
    request.set_protocol_type(protocolType)
    request.set_method(method)
    request.set_uri_pattern(uri)
    request.add_header('Content-Type', 'application/json')
    return request

def init_parameters():
    """初始化请求参数"""
    root = dict()
    root['AppKey'] = '输入您在听悟管控台创建的Appkey'
    
    # 基本请求参数
    input = dict()
    input['SourceLanguage'] = 'cn'
    input['TaskKey'] = 'task' + datetime.datetime.now().strftime('%Y%m%d%H%M%S')
    input['FileUrl'] = '输入待测试的音频url链接'
    root['Input'] = input
    
    # AI相关参数
    parameters = dict()
    
    # 口语书面化
    parameters['TextPolishEnabled'] = True
    
    root['Parameters'] = parameters
    return root

# 执行流程
body = init_parameters()
print(body)

credentials = AccessKeyCredential(
    os.environ['ALIBABA_CLOUD_ACCESS_KEY_ID'], 
    os.environ['ALIBABA_CLOUD_ACCESS_KEY_SECRET']
)
client = AcsClient(region_id='cn-beijing', credential=credentials)

request = create_common_request(
    'tingwu.cn-beijing.aliyuncs.com', 
    '2023-09-30', 
    'https', 
    'PUT', 
    '/openapi/tingwu/v2/tasks'
)
request.add_query_param('type', 'offline')
request.set_content(json.dumps(body).encode('utf-8'))

response = client.do_action_with_exception(request)
print("response: \n" + json.dumps(json.loads(response), indent=4, ensure_ascii=False))
```

---

## 高级功能

### 8. 身份识别

**功能说明**: 根据场景和身份描述，自动识别对话中的不同角色

**适用场景**: 销售对话分析、客服质检、面试录音

**参数配置**:
- `IdentityRecognitionEnabled: True` - 启用身份识别
- `SceneIntroduction` - 场景描述
- `IdentityContents` - 身份列表及描述

```python
#!/usr/bin/env python
# coding=utf-8

import os
import json
import datetime
from aliyunsdkcore.client import AcsClient
from aliyunsdkcore.request import CommonRequest
from aliyunsdkcore.auth.credentials import AccessKeyCredential

def create_common_request(domain, version, protocolType, method, uri):
    """创建通用请求对象"""
    request = CommonRequest()
    request.set_accept_format('json')
    request.set_domain(domain)
    request.set_version(version)
    request.set_protocol_type(protocolType)
    request.set_method(method)
    request.set_uri_pattern(uri)
    request.add_header('Content-Type', 'application/json')
    return request

def init_parameters():
    """初始化请求参数"""
    root = dict()
    root['AppKey'] = '输入您在听悟管控台创建的Appkey'
    
    # 基本请求参数
    input = dict()
    input['SourceLanguage'] = 'cn'
    input['TaskKey'] = 'task' + datetime.datetime.now().strftime('%Y%m%d%H%M%S')
    input['FileUrl'] = '输入待测试的音频url链接'
    root['Input'] = input
    
    # AI相关参数
    parameters = dict()
    
    # 身份识别
    parameters['IdentityRecognitionEnabled'] = True
    identity_recognition = {
        "SceneIntroduction": "汽车门店线下销售场景",
        "IdentityContents": [
            {
                "Name": "销售",
                "Description": "介绍车辆的不同配置、性能与技术、舒适性与便利性等"
            },
            {
                "Name": "客户",
                "Description": "对车辆提出疑问，表达使用感受等"
            }
        ]
    }
    parameters['IdentityRecognition'] = identity_recognition
    
    root['Parameters'] = parameters
    return root

# 执行流程
body = init_parameters()
print(body)

credentials = AccessKeyCredential(
    os.environ['ALIBABA_CLOUD_ACCESS_KEY_ID'], 
    os.environ['ALIBABA_CLOUD_ACCESS_KEY_SECRET']
)
client = AcsClient(region_id='cn-beijing', credential=credentials)

request = create_common_request(
    'tingwu.cn-beijing.aliyuncs.com', 
    '2023-09-30', 
    'https', 
    'PUT', 
    '/openapi/tingwu/v2/tasks'
)
request.add_query_param('type', 'offline')
request.set_content(json.dumps(body).encode('utf-8'))

response = client.do_action_with_exception(request)
print("response: \n" + json.dumps(json.loads(response), indent=4, ensure_ascii=False))
```

---

### 9. 对话内容提取

**功能说明**: 从对话中提取特定主题的内容，如价格异议、竞品反馈、销售话术等

**适用场景**: 销售对话分析、客户需求提取、竞品情报收集

**参数配置**:
- `ContentExtractionEnabled: True` - 启用内容提取
- `SceneIntroduction` - 场景描述
- `ExtractionContents` - 提取内容列表

```python
#!/usr/bin/env python
# coding=utf-8

import os
import json
import datetime
from aliyunsdkcore.client import AcsClient
from aliyunsdkcore.request import CommonRequest
from aliyunsdkcore.auth.credentials import AccessKeyCredential

def create_common_request(domain, version, protocolType, method, uri):
    """创建通用请求对象"""
    request = CommonRequest()
    request.set_accept_format('json')
    request.set_domain(domain)
    request.set_version(version)
    request.set_protocol_type(protocolType)
    request.set_method(method)
    request.set_uri_pattern(uri)
    request.add_header('Content-Type', 'application/json')
    return request

def init_parameters():
    """初始化请求参数"""
    root = dict()
    root['AppKey'] = '输入您在听悟管控台创建的Appkey'
    
    # 基本请求参数
    input = dict()
    input['SourceLanguage'] = 'cn'
    input['TaskKey'] = 'task' + datetime.datetime.now().strftime('%Y%m%d%H%M%S')
    input['FileUrl'] = '输入待测试的音频url链接'
    root['Input'] = input
    
    # AI相关参数
    parameters = dict()
    
    # 对话内容提取
    parameters['ContentExtractionEnabled'] = True
    content_extraction = {
        "SceneIntroduction": "汽车门店线下销售场景",
        "ExtractionContents": [
            {
                "Title": "客户价格异议",
                "Content": "客户提到对车型价格、优惠活动或担心之后降价",
                "Identity": "客户"
            },
            {
                "Title": "竞品车型反馈",
                "Content": "总结客户明确表达感受的所有竞品品牌或竞品车型，并总结客户对竞品品牌或车型的想法",
                "Identity": "客户"
            },
            {
                "Title": "引导购车话术",
                "Content": "基于对话提取销售引导客户购车的话术",
                "Identity": "销售"
            }
        ]
    }
    parameters['ContentExtraction'] = content_extraction
    
    root['Parameters'] = parameters
    return root

# 执行流程
body = init_parameters()
print(body)

credentials = AccessKeyCredential(
    os.environ['ALIBABA_CLOUD_ACCESS_KEY_ID'], 
    os.environ['ALIBABA_CLOUD_ACCESS_KEY_SECRET']
)
client = AcsClient(region_id='cn-beijing', credential=credentials)

request = create_common_request(
    'tingwu.cn-beijing.aliyuncs.com', 
    '2023-09-30', 
    'https', 
    'PUT', 
    '/openapi/tingwu/v2/tasks'
)
request.add_query_param('type', 'offline')
request.set_content(json.dumps(body).encode('utf-8'))

response = client.do_action_with_exception(request)
print("response: \n" + json.dumps(json.loads(response), indent=4, ensure_ascii=False))
```

---

### 10. 服务质检

**功能说明**: 检测服务对话中的规范执行情况，如问候语、饮品提供、留资等

**适用场景**: 客服质检、销售规范检查、服务标准评估

**参数配置**:
- `ServiceInspectionEnabled: True` - 启用服务质检
- `SceneIntroduction` - 场景描述
- `InspectionIntroduction` - 质检总体说明
- `InspectionContents` - 质检项列表

```python
#!/usr/bin/env python
# coding=utf-8

import os
import json
import datetime
from aliyunsdkcore.client import AcsClient
from aliyunsdkcore.request import CommonRequest
from aliyunsdkcore.auth.credentials import AccessKeyCredential

def create_common_request(domain, version, protocolType, method, uri):
    """创建通用请求对象"""
    request = CommonRequest()
    request.set_accept_format('json')
    request.set_domain(domain)
    request.set_version(version)
    request.set_protocol_type(protocolType)
    request.set_method(method)
    request.set_uri_pattern(uri)
    request.add_header('Content-Type', 'application/json')
    return request

def init_parameters():
    """初始化请求参数"""
    root = dict()
    root['AppKey'] = '输入您在听悟管控台创建的Appkey'
    
    # 基本请求参数
    input = dict()
    input['SourceLanguage'] = 'cn'
    input['TaskKey'] = 'task' + datetime.datetime.now().strftime('%Y%m%d%H%M%S')
    input['FileUrl'] = '输入待测试的音频url链接'
    root['Input'] = input
    
    # AI相关参数
    parameters = dict()
    
    # 服务质检
    parameters['ServiceInspectionEnabled'] = True
    service_inspection = {
        "SceneIntroduction": "汽车门店线下销售场景",
        "InspectionIntroduction": "请检测对话中汽车销售人员表现是否接待热情、态度良好",
        "InspectionContents": [
            {
                "Title": "到店迎接-欢迎语",
                "Content": "销售在开场白的时候主动向客户打招呼进行欢迎"
            },
            {
                "Title": "离店送别-客户留资",
                "Content": "销售邀请客户留下微信、电话号码、名片等联系方式"
            },
            {
                "Title": "到店迎接-饮品提供",
                "Content": "销售在接待客户的时候主动询问是否需要饮料（如咖啡、橙汁、水、茶等）、点心、零食、水果等"
            }
        ]
    }
    parameters['ServiceInspection'] = service_inspection
    
    root['Parameters'] = parameters
    return root

# 执行流程
body = init_parameters()
print(body)

credentials = AccessKeyCredential(
    os.environ['ALIBABA_CLOUD_ACCESS_KEY_ID'], 
    os.environ['ALIBABA_CLOUD_ACCESS_KEY_SECRET']
)
client = AcsClient(region_id='cn-beijing', credential=credentials)

request = create_common_request(
    'tingwu.cn-beijing.aliyuncs.com', 
    '2023-09-30', 
    'https', 
    'PUT', 
    '/openapi/tingwu/v2/tasks'
)
request.add_query_param('type', 'offline')
request.set_content(json.dumps(body).encode('utf-8'))

response = client.do_action_with_exception(request)
print("response: \n" + json.dumps(json.loads(response), indent=4, ensure_ascii=False))
```

---

### 11. 自定义Prompt

**功能说明**: 使用自定义Prompt对转写结果进行个性化分析处理

**适用场景**: 特殊业务需求、自定义分析逻辑、灵活内容提取

**参数配置**:
- `CustomPromptEnabled: true` - 启用自定义Prompt
- `Contents` - Prompt内容列表
  - `Name` - Prompt名称
  - `Prompt` - Prompt文本（可使用 `{Transcription}` 占位符）
  - `Model` - 使用的模型（如 `tingwu-turbo`）
  - `TransType` - 转换类型（`chat` 或 `default`）

**JSON配置示例**:

```json
{
    "Input": {
        "SourceLanguage": "cn",
        "TaskKey": "task20251028...",
        "FileUrl": "https://your-oss-url.mp3"
    },
    "Parameters": {
        "CustomPromptEnabled": true,
        "CustomPrompt": {
            "Contents": [
                {
                    "Name": "split-summary-demo",
                    "Prompt": "请帮我将下面的对话进行总结，根据发言人来总结:\n {Transcription}",
                    "Model": "tingwu-turbo",
                    "TransType": "chat"
                },
                {
                    "Name": "inspection-demo",
                    "Prompt": "请帮我检查对话内容是否存在不文明用语，如果有请回复'有'，如果没有请回复'无'，对话内容如下:\n {Transcription}",
                    "Model": "tingwu-turbo",
                    "TransType": "default"
                }
            ]
        }
    }
}
```

**Python实现示例**:

```python
#!/usr/bin/env python
# coding=utf-8

import os
import json
import datetime
from aliyunsdkcore.client import AcsClient
from aliyunsdkcore.request import CommonRequest
from aliyunsdkcore.auth.credentials import AccessKeyCredential

def create_common_request(domain, version, protocolType, method, uri):
    """创建通用请求对象"""
    request = CommonRequest()
    request.set_accept_format('json')
    request.set_domain(domain)
    request.set_version(version)
    request.set_protocol_type(protocolType)
    request.set_method(method)
    request.set_uri_pattern(uri)
    request.add_header('Content-Type', 'application/json')
    return request

def init_parameters():
    """初始化请求参数"""
    root = dict()
    root['AppKey'] = '输入您在听悟管控台创建的Appkey'
    
    # 基本请求参数
    input = dict()
    input['SourceLanguage'] = 'cn'
    input['TaskKey'] = 'task' + datetime.datetime.now().strftime('%Y%m%d%H%M%S')
    input['FileUrl'] = '输入待测试的音频url链接'
    root['Input'] = input
    
    # AI相关参数
    parameters = dict()
    
    # 自定义Prompt
    parameters['CustomPromptEnabled'] = True
    parameters['CustomPrompt'] = {
        "Contents": [
            {
                "Name": "split-summary-demo",
                "Prompt": "请帮我将下面的对话进行总结，根据发言人来总结:\n {Transcription}",
                "Model": "tingwu-turbo",
                "TransType": "chat"
            },
            {
                "Name": "inspection-demo",
                "Prompt": "请帮我检查对话内容是否存在不文明用语，如果有请回复'有'，如果没有请回复'无'，对话内容如下:\n {Transcription}",
                "Model": "tingwu-turbo",
                "TransType": "default"
            }
        ]
    }
    
    root['Parameters'] = parameters
    return root

# 执行流程
body = init_parameters()
print(json.dumps(body, indent=2, ensure_ascii=False))

credentials = AccessKeyCredential(
    os.environ['ALIBABA_CLOUD_ACCESS_KEY_ID'], 
    os.environ['ALIBABA_CLOUD_ACCESS_KEY_SECRET']
)
client = AcsClient(region_id='cn-beijing', credential=credentials)

request = create_common_request(
    'tingwu.cn-beijing.aliyuncs.com', 
    '2023-09-30', 
    'https', 
    'PUT', 
    '/openapi/tingwu/v2/tasks'
)
request.add_query_param('type', 'offline')
request.set_content(json.dumps(body).encode('utf-8'))

response = client.do_action_with_exception(request)
print("response: \n" + json.dumps(json.loads(response), indent=4, ensure_ascii=False))
```

---

## 公共方法说明

### create_common_request()

创建通用API请求对象，所有示例代码共用此方法。

**参数**:
- `domain`: API域名 (如 `tingwu.cn-beijing.aliyuncs.com`)
- `version`: API版本 (如 `2023-09-30`)
- `protocolType`: 协议类型 (固定为 `https`)
- `method`: HTTP方法 (提交任务用 `PUT`，查询用 `GET`)
- `uri`: API路径 (如 `/openapi/tingwu/v2/tasks`)

**返回**: CommonRequest对象

### init_parameters()

初始化API请求参数，根据不同功能设置不同的参数。

**核心参数结构**:

```python
{
    "AppKey": "your-appkey",           # 必需：听悟AppKey
    "Input": {
        "SourceLanguage": "cn",        # 必需：源语言
        "TaskKey": "task20251028...",  # 必需：唯一任务标识
        "FileUrl": "https://..."       # 必需：音频文件OSS URL
    },
    "Parameters": {
        # 根据功能需求配置不同的参数
    }
}
```

---

## API参数说明

### 基础参数 (Input)

| 参数 | 类型 | 必需 | 说明 |
|------|------|------|------|
| AppKey | String | 是 | 听悟控制台创建的AppKey |
| SourceLanguage | String | 是 | 源语言代码，如 `cn`（中文）、`en`（英文） |
| TaskKey | String | 是 | 唯一任务标识符，建议使用时间戳 |
| FileUrl | String | 是 | 音频文件的OSS公网URL（HTTPS） |

### 功能参数 (Parameters)

#### 语音转写
- `Transcription.DiarizationEnabled`: Boolean - 启用角色分离
- `Transcription.Diarization.SpeakerCount`: Integer - 说话人数量

#### 文本翻译
- `TranslationEnabled`: Boolean - 启用翻译
- `Translation.TargetLanguages`: Array - 目标语言列表

#### 章节速览
- `AutoChaptersEnabled`: Boolean - 启用章节速览
- `AutoChapters.ChapterGranularity`: String - 章节粒度（`Coarse`/`Fine`）
- `AutoChapters.TitleLengthLevel`: String - 标题长度（`Short`/`Medium`/`Long`）

#### 摘要总结
- `PptExtractionEnabled`: Boolean - 启用PPT抽取

#### 要点提炼
- `MeetingAssistanceEnabled`: Boolean - 启用会议助手
- `MeetingAssistance.Types`: Array - 提取类型（`Actions`, `KeyInformation`）

#### 口语书面化
- `TextPolishEnabled`: Boolean - 启用文本润色

#### 身份识别
- `IdentityRecognitionEnabled`: Boolean - 启用身份识别
- `IdentityRecognition.SceneIntroduction`: String - 场景描述
- `IdentityRecognition.IdentityContents`: Array - 身份列表

#### 对话内容提取
- `ContentExtractionEnabled`: Boolean - 启用内容提取
- `ContentExtraction.SceneIntroduction`: String - 场景描述
- `ContentExtraction.ExtractionContents`: Array - 提取内容列表

#### 服务质检
- `ServiceInspectionEnabled`: Boolean - 启用服务质检
- `ServiceInspection.SceneIntroduction`: String - 场景描述
- `ServiceInspection.InspectionIntroduction`: String - 质检说明
- `ServiceInspection.InspectionContents`: Array - 质检项列表

#### 自定义Prompt
- `CustomPromptEnabled`: Boolean - 启用自定义Prompt
- `CustomPrompt.Contents`: Array - Prompt列表

---

## 常见问题 (FAQ)

### Q1: 音频文件格式要求？
**A**: 支持 MP3、WAV、M4A、FLAC、AAC 等常见格式，建议使用 MP3 或 WAV。

### Q2: 音频文件大小限制？
**A**: 单文件最大 2GB，时长最长 5 小时。

### Q3: 如何获取任务结果？
**A**: 提交任务后，使用返回的 `TaskId` 通过 GET 方法轮询查询任务状态和结果。

### Q4: 转写准确率如何提高？
**A**: 
- 使用清晰的音频文件（采样率 16kHz 以上）
- 减少背景噪音
- 启用角色分离功能
- 提供准确的场景描述

### Q5: 支持实时转写吗？
**A**: 本文档示例均为离线转写（`type=offline`），实时转写需使用不同的API接口。

---

## 相关资源

- **官方文档**: https://help.aliyun.com/zh/tingwu/
- **控制台**: https://tingwu.aliyun.com/console
- **AccessKey管理**: https://ram.console.aliyun.com/manage/ak
- **技术支持**: 提交工单或访问开发者社区

---

## 更新日志

| 日期 | 版本 | 说明 |
|------|------|------|
| 2025-10-28 | 1.1.0 | 优化文档排版，添加目录和详细说明 |
| 2025-10-24 | 1.0.0 | 初始版本，包含所有官方示例代码 |

---

**注意事项**:
1. 所有示例代码均需替换 `AppKey` 和 `FileUrl` 为实际值
2. 确保音频文件已上传至OSS并设置为公开访问
3. 建议在测试环境先验证功能后再用于生产环境
4. 遵守阿里云服务使用条款和数据安全规范

---

*本文档由阿里云官方提供，仅供参考。如有疑问，请查阅最新官方文档。*
