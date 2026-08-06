#!/bin/bash

# AI Assistant - 一键推送到 GitHub 脚本
# 使用方法: ./push-to-github.sh <您的GitHub用户名> <仓库名> <GitHub令牌>

if [ "$#" -ne 3 ]; then
    echo "用法: $0 <GITHUB_USERNAME> <REPO_NAME> <GITHUB_TOKEN>"
    echo "示例: $0 myusername ai-assistant ghp_xxxxxxxxxxxx"
    exit 1
fi

USERNAME=$1
REPO_NAME=$2
TOKEN=$3
REPO_URL="https://${TOKEN}@github.com/${USERNAME}/${REPO_NAME}.git"

echo "🚀 开始推送代码到 GitHub..."
echo "仓库地址: https://github.com/${USERNAME}/${REPO_NAME}"

# 初始化 Git (如果尚未初始化)
if [ ! -d ".git" ]; then
    echo "📦 初始化 Git 仓库..."
    git init
    git branch -M main
fi

# 添加所有文件并提交
echo "💾 提交代码..."
git add .
git commit -m "feat: 完整的 AI 智能助手项目 (记忆系统+RAG+多人格)" || echo "✅ 没有需要提交的更改"

# 设置远程仓库
echo "🔗 配置远程仓库..."
git remote remove origin 2>/dev/null || true
git remote add origin "$REPO_URL"

# 推送到 GitHub
echo "⬆️ 推送到 GitHub..."
git push -u origin main --force

if [ $? -eq 0 ]; then
    echo ""
    echo "✅ 推送成功!"
    echo "🌐 访问您的仓库: https://github.com/${USERNAME}/${REPO_NAME}"
    echo ""
    echo "⚠️  安全提醒: 请立即前往 GitHub 撤销使用的令牌!"
    echo "   链接: https://github.com/settings/tokens"
else
    echo ""
    echo "❌ 推送失败，请检查:"
    echo "   1. 令牌是否有效"
    echo "   2. 仓库名称是否正确"
    echo "   3. 网络连接是否正常"
    exit 1
fi
