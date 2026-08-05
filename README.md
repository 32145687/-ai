# AI Assistant - 个人智能助手 Android 应用

一个完全本地化的 AI 智能助手，具备多人格、长期记忆、学习习惯和提醒功能。

## 🌟 核心特性

### 1. 多人格聊天系统
- **独立记忆**: 每个人格拥有独立的对话历史和记忆
- **预设人格**: 
  - 暖心伙伴 - 温暖友善的聊天伙伴
  - 专业顾问 - 专业严谨的顾问
  - 创意伙伴 - 富有创意的合作伙伴
  - 共情倾听者 - 深度共情的倾听者
  - 分析思考者 - 理性分析的思考者
- **自定义人格**: 支持创建和定制专属人格

### 2. 长期记忆系统
- **向量数据库**: 使用 Room + 自定义 embedding 存储实现语义搜索
- **记忆类型**:
  - 对话历史
  - 用户事实 (姓名、偏好等)
  - 行为习惯
  - 学习知识
  - 提醒任务
  - 情绪状态
- **RAG 检索**: 基于重要性和相关性自动检索相关记忆

### 3. 学习与适应
- **行为模式识别**:
  - 活跃时间分析
  - 沟通风格学习
  - 话题兴趣追踪
  - 响应偏好记忆
- **自适应改进**: 根据交互持续优化响应质量

### 4. 智能提醒
- **灵活调度**: 支持一次性、每日、每周、每月重复
- **优先级管理**: 低/中/高/紧急四级优先级
- **人格关联**: 可为特定人格创建专属提醒

### 5. 隐私保护
- **完全本地化**: 所有数据存储在本地数据库
- **云端仅用于模型调用**: 仅在调用 LLM API 时联网
- **数据加密**: 敏感信息加密存储

## 🏗️ 技术架构

### 技术栈
- **语言**: Kotlin
- **UI**: Jetpack Compose + Material Design 3
- **架构**: MVVM + Clean Architecture
- **依赖注入**: Hilt
- **数据库**: Room (SQLite)
- **数据存储**: DataStore Preferences
- **网络**: Retrofit + OkHttp
- **异步**: Kotlin Coroutines & Flow

### 项目结构
```
app/
├── src/main/java/com/myai/assistant/
│   ├── data/
│   │   ├── local/          # 本地数据层
│   │   │   ├── dao/        # Room DAOs
│   │   │   ├── entity/     # 数据库实体
│   │   │   └── database/   # Room Database
│   │   ├── remote/         # 远程数据层
│   │   │   ├── api/        # Retrofit APIs
│   │   │   └── model/      # API 数据模型
│   │   └── manager/        # 数据管理器 (SettingsManager)
│   ├── domain/
│   │   ├── model/          # 领域模型
│   │   ├── repository/     # 仓库接口实现
│   │   └── usecase/        # 业务逻辑用例
│   ├── ui/
│   │   ├── theme/          # Compose 主题
│   │   ├── components/     # 可复用组件
│   │   └── screens/        # 界面屏幕
│   │       ├── chat/       # 聊天界面
│   │       ├── persona/    # 人格管理
│   │       ├── settings/   # 设置界面
│   │       └── memory/     # 记忆管理
│   └── di/                 # Hilt 依赖注入模块
└── build.gradle.kts
```

## 🚀 快速开始

### 环境要求
- Android Studio Hedgehog (2023.1.1) 或更高版本
- JDK 17
- Android SDK 34
- 最低支持 Android 8.0 (API 26)

### 构建步骤

1. **克隆项目**
```bash
git clone <repository-url>
cd AIAssistant
```

2. **配置 API 密钥**
   - 首次启动应用后，进入设置页面
   - 输入你的 OpenAI API Key 或其他支持的 LLM API 密钥

3. **构建 APK**
```bash
./gradlew assembleDebug
# 或发布版本
./gradlew assembleRelease
```

4. **安装到设备**
```bash
adb install app/build/outputs/apk/debug/app-debug.apk
```

## 📱 使用指南

### 配置 API
1. 打开应用 → 设置
2. 输入 API 密钥 (支持 OpenAI、Claude 等)
3. 选择默认模型 (gpt-4, gpt-3.5-turbo 等)

### 切换人格
1. 点击顶部人格选择器
2. 从列表中选择不同人格
3. 每个人格的对话记录完全独立

### 创建提醒
1. 在聊天中输入"提醒我..."
2. AI 会自动解析并创建提醒
3. 或在提醒页面手动创建

### 查看记忆
1. 进入记忆页面
2. 可按类型筛选 (事实/习惯/偏好等)
3. 支持关键词搜索

## 🔧 GitHub Actions 自动构建

项目已配置 GitHub Actions，推送代码后自动构建 APK:

1. 在 GitHub 仓库启用 Actions
2. 推送代码触发工作流
3. 在 Actions 页面下载生成的 APK

## 📝 待开发功能

- [ ] 完整的向量嵌入生成与搜索
- [ ] 更智能的行为模式学习算法
- [ ] 本地小模型推理支持 (MLC LLM)
- [ ] 数据导出/备份功能
- [ ] 语音输入/输出
- [ ] 更多预设人格
- [ ] 人格训练/微调功能
- [ ] 多语言支持

## ⚠️ 注意事项

1. **API 费用**: 使用云端模型会产生 API 调用费用
2. **存储空间**: 长期使用会占用较多本地存储空间
3. **电池消耗**: 后台提醒服务会轻微增加电量消耗
4. **网络需求**: 需要网络连接才能调用云端模型

## 📄 许可证

本项目仅供个人学习和使用。

---

**开发者**: 个人项目  
**版本**: 1.0.0  
**最后更新**: 2024
