#!/usr/bin/env bash
#
# setup_skill.sh — 在 Android Studio 專案中部署 android-clean-arch skill
# 支援 GitHub Copilot、Claude Code、Cursor、Codex 同時使用
#
# ============================================================
#  用法（三種都可以，效果一樣）：
#
#  1) 在專案根目錄執行：
#     cd ~/my-android-app
#     bash ~/Downloads/android-clean-arch/setup_skill.sh
#
#  2) 進入 skill 資料夾後指定專案路徑：
#     cd ~/Downloads/android-clean-arch
#     bash setup_skill.sh --project ~/my-android-app
#
#  3) 完整指定兩個路徑：
#     bash setup_skill.sh --project ~/my-android-app
# ============================================================

set -euo pipefail

GREEN='\033[0;32m'
CYAN='\033[0;36m'
YELLOW='\033[1;33m'
NC='\033[0m'

SKILL_NAME="android-clean-arch"

# ─── 定位 Skill 來源（用腳本自身位置推算） ───
SCRIPT_DIR="$(cd "$(dirname "${BASH_SOURCE[0]}")" && pwd)"

# 判斷 SKILL.md 是否在腳本同目錄
if [ -f "${SCRIPT_DIR}/SKILL.md" ]; then
  SKILL_SOURCE="${SCRIPT_DIR}"
elif [ -f "${SCRIPT_DIR}/${SKILL_NAME}/SKILL.md" ]; then
  SKILL_SOURCE="${SCRIPT_DIR}/${SKILL_NAME}"
else
  echo -e "${YELLOW}⚠️  找不到 SKILL.md${NC}"
  echo "   腳本位置：${SCRIPT_DIR}"
  echo "   請確認 setup_skill.sh 和 SKILL.md 在同一個 ${SKILL_NAME}/ 資料夾內"
  exit 1
fi

echo -e "${CYAN}=== Android Clean Architecture Skill 安裝 ===${NC}"
echo ""
echo "📦 Skill 來源：${SKILL_SOURCE}"

# ─── 定位專案根目錄 ───
PROJECT_DIR=""

# 優先使用 --project 參數
while [[ $# -gt 0 ]]; do
  case $1 in
    --project|-p)
      PROJECT_DIR="$(cd "$2" && pwd)"
      shift 2
      ;;
    *)
      shift
      ;;
  esac
done

# 如果沒指定 --project，嘗試用 CWD
if [ -z "$PROJECT_DIR" ]; then
  PROJECT_DIR="$(pwd)"
fi

# 驗證是否為 Android 專案
if [ -f "${PROJECT_DIR}/build.gradle.kts" ] || \
   [ -f "${PROJECT_DIR}/build.gradle" ] || \
   [ -f "${PROJECT_DIR}/settings.gradle.kts" ] || \
   [ -f "${PROJECT_DIR}/settings.gradle" ]; then
  echo -e "${GREEN}✅ 偵測到 Android 專案：${PROJECT_DIR}${NC}"
else
  echo -e "${YELLOW}⚠️  在 ${PROJECT_DIR} 找不到 Gradle 設定檔${NC}"
  echo ""
  echo "   請用以下方式指定專案路徑："
  echo "   bash ${BASH_SOURCE[0]} --project /path/to/your/android-project"
  echo ""
  echo "   或先 cd 到專案根目錄再執行："
  echo "   cd /path/to/your/android-project"
  echo "   bash ${BASH_SOURCE[0]}"
  exit 1
fi

cd "$PROJECT_DIR"

# ─── Step 1: 建立標準路徑 (.agents/skills/) ───
STANDARD_PATH=".agents/skills/${SKILL_NAME}"

if [ -f "${STANDARD_PATH}/SKILL.md" ]; then
  echo "📁 ${STANDARD_PATH}/ 已存在且包含 SKILL.md，跳過複製"
else
  echo "📁 建立 ${STANDARD_PATH}/ ..."
  mkdir -p ".agents/skills"

  # 避免把自己複製到自己裡面
  REAL_SOURCE="$(cd "${SKILL_SOURCE}" && pwd)"
  REAL_TARGET="$(mkdir -p "${STANDARD_PATH}" && cd "${STANDARD_PATH}" && pwd)"

  if [ "${REAL_SOURCE}" = "${REAL_TARGET}" ]; then
    echo "   來源與目標相同，跳過複製"
  else
    # 複製 skill 內容（排除 setup_skill.sh 自身）
    rm -rf "${STANDARD_PATH}"
    mkdir -p "${STANDARD_PATH}"
    cp "${SKILL_SOURCE}/SKILL.md" "${STANDARD_PATH}/"

    # 複製子目錄
    for dir in scripts references assets; do
      if [ -d "${SKILL_SOURCE}/${dir}" ]; then
        cp -r "${SKILL_SOURCE}/${dir}" "${STANDARD_PATH}/"
      fi
    done
  fi
fi

echo -e "${GREEN}✅ 標準路徑就緒：${STANDARD_PATH}/${NC}"
echo "   → GitHub Copilot, Codex, Windsurf 會自動偵測"

# ─── Step 2: Claude Code symlink ───
CLAUDE_PATH=".claude/skills/${SKILL_NAME}"

if [ -L "$CLAUDE_PATH" ] || [ -d "$CLAUDE_PATH" ]; then
  echo "📁 ${CLAUDE_PATH} 已存在，跳過"
else
  echo "🔗 建立 Claude Code symlink ..."
  mkdir -p ".claude/skills"
  ln -s "../../.agents/skills/${SKILL_NAME}" "$CLAUDE_PATH"
  echo -e "${GREEN}✅ Claude Code symlink 就緒${NC}"
fi

# ─── Step 3: Cursor symlink ───
CURSOR_PATH=".cursor/skills/${SKILL_NAME}"

if [ -L "$CURSOR_PATH" ] || [ -d "$CURSOR_PATH" ]; then
  echo "📁 ${CURSOR_PATH} 已存在，跳過"
else
  echo "🔗 建立 Cursor symlink ..."
  mkdir -p ".cursor/skills"
  ln -s "../../.agents/skills/${SKILL_NAME}" "$CURSOR_PATH"
  echo -e "${GREEN}✅ Cursor symlink 就緒${NC}"
fi

# ─── Step 4: 更新 .gitignore ───
GITIGNORE=".gitignore"
MARKER="# AI Agent skills (auto-generated)"

if [ -f "$GITIGNORE" ] && grep -qF "$MARKER" "$GITIGNORE" 2>/dev/null; then
  echo "📄 .gitignore 已包含 Agent 設定，跳過"
else
  echo "📄 更新 .gitignore ..."
  cat >> "$GITIGNORE" <<'EOF'

# AI Agent skills (auto-generated)
# 實體 skill 檔案在 .agents/skills/ — 這要進 git
# 其他 agent 設定目錄只保留 skills symlinks
.claude/*
!.claude/skills
.cursor/*
!.cursor/skills
EOF
  echo -e "${GREEN}✅ .gitignore 已更新${NC}"
fi

# ─── 完成 ───
echo ""
echo -e "${CYAN}=== 安裝完成 ===${NC}"
echo ""
echo "  ${PROJECT_DIR}/"
echo "  ├── .agents/skills/"
echo "  │   └── ${SKILL_NAME}/      ← 實體檔案 (進 git)"
echo "  │       ├── SKILL.md"
echo "  │       ├── scripts/"
echo "  │       └── references/"
echo "  ├── .claude/skills/"
echo "  │   └── ${SKILL_NAME} → symlink"
echo "  ├── .cursor/skills/"
echo "  │   └── ${SKILL_NAME} → symlink"
echo "  └── ..."
echo ""
echo "🎯 支援的 Agent:"
echo "   • GitHub Copilot  → .agents/skills/"
echo "   • OpenAI Codex    → .agents/skills/"
echo "   • Claude Code     → .claude/skills/"
echo "   • Cursor          → .cursor/skills/"
echo ""
echo -e "💡 下一步：${GREEN}git add .agents/skills/${NC}"
echo "   將 skill 納入版控，團隊成員 clone 後自動取得"
