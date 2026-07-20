<script setup>
import { ref, nextTick, watch, onUnmounted } from 'vue'
import { chatApi } from '@/api/agent'
import { ChatDotRound } from '@element-plus/icons-vue'

const visible = ref(false)
const input = ref('')
const sending = ref(false)
const messages = ref([
  { role: 'assistant', content: '你好！我是拾光计划的智能助手。你可以让我帮你创建目标、打卡记录、查看统计数据等。试试跟我说「帮我建一个学英语的目标」吧！', time: '' }
])
const chatBodyRef = ref(null)
const panelWidth = ref(380)
const panelHeight = ref(520)

// 拖拽放缩
const resizing = ref(false)
let resizeStartX = 0
let resizeStartY = 0
let resizeStartW = 0
let resizeStartH = 0

const onResizeStart = (e) => {
  resizing.value = true
  resizeStartX = e.clientX
  resizeStartY = e.clientY
  resizeStartW = panelWidth.value
  resizeStartH = panelHeight.value
  document.addEventListener('mousemove', onResizeMove)
  document.addEventListener('mouseup', onResizeEnd)
  e.preventDefault()
}

const onResizeMove = (e) => {
  if (!resizing.value) return
  // 向左/上拖动放大，向右/下拖动缩小（面板锚定右下角，左上角拖动）
  const dx = resizeStartX - e.clientX
  const dy = resizeStartY - e.clientY
  panelWidth.value = Math.min(800, Math.max(300, resizeStartW + dx))
  panelHeight.value = Math.min(window.innerHeight - 100, Math.max(350, resizeStartH + dy))
}

const onResizeEnd = () => {
  resizing.value = false
  document.removeEventListener('mousemove', onResizeMove)
  document.removeEventListener('mouseup', onResizeEnd)
}

onUnmounted(() => {
  document.removeEventListener('mousemove', onResizeMove)
  document.removeEventListener('mouseup', onResizeEnd)
})

const toggle = () => {
  visible.value = !visible.value
}

const scrollToBottom = () => {
  nextTick(() => {
    if (chatBodyRef.value) {
      chatBodyRef.value.scrollTop = chatBodyRef.value.scrollHeight
    }
  })
}

const formatTime = () => {
  const now = new Date()
  return now.getHours().toString().padStart(2, '0') + ':' + now.getMinutes().toString().padStart(2, '0')
}

const send = async () => {
  const text = input.value.trim()
  if (!text || sending.value) return

  messages.value.push({ role: 'user', content: text, time: formatTime() })
  input.value = ''
  scrollToBottom()

  sending.value = true
  try {
    // 发送完整对话历史（不含欢迎语），LLM 可获得上下文
    const history = messages.value
      .filter(m => m.role !== 'assistant' || m.time !== '') // 排除初始欢迎消息
      .map(m => ({ role: m.role, content: m.content }))
    const data = await chatApi(history)
    const reply = data?.reply || '操作完成'
    messages.value.push({ role: 'assistant', content: reply, time: formatTime() })
  } catch {
    messages.value.push({ role: 'assistant', content: '抱歉，网络出问题了，请稍后再试。', time: formatTime() })
  } finally {
    sending.value = false
    scrollToBottom()
  }
}

watch(visible, (val) => {
  if (val) {
    nextTick(() => {
      if (chatBodyRef.value) {
        chatBodyRef.value.scrollTop = chatBodyRef.value.scrollHeight
      }
    })
  }
})
</script>

<template>
  <div class="floating-chat">
    <!-- Toggle button -->
    <div class="chat-toggle" @click="toggle">
      <el-icon :size="24"><ChatDotRound /></el-icon>
    </div>

    <!-- Chat panel -->
    <Transition name="chat-slide">
      <div v-if="visible" class="chat-panel" :style="{ width: panelWidth + 'px', height: panelHeight + 'px' }" :class="{ resizing: resizing }">
        <!-- 拖拽放缩手柄 -->
        <div class="resize-handle" @mousedown="onResizeStart">
          <svg width="12" height="12" viewBox="0 0 12 12"><path d="M0 12L12 0M4 12L12 4M8 12L12 8" stroke="#c0c4cc" stroke-width="1.5" fill="none"/></svg>
        </div>
        <div class="chat-header">
          <span>拾光助手</span>
          <el-button link @click="visible = false">
            <el-icon :size="18"><Close /></el-icon>
          </el-button>
        </div>

        <div ref="chatBodyRef" class="chat-body">
          <div v-for="(msg, idx) in messages" :key="idx" :class="['chat-message', msg.role]">
            <div class="chat-bubble">
              <div class="chat-text">{{ msg.content }}</div>
              <div class="chat-time">{{ msg.time }}</div>
            </div>
          </div>
          <div v-if="sending" class="chat-message assistant">
            <div class="chat-bubble typing">
              <span class="dot"></span><span class="dot"></span><span class="dot"></span>
            </div>
          </div>
        </div>

        <div class="chat-footer">
          <el-input
            v-model="input"
            type="textarea"
            :rows="2"
            placeholder="输入你的问题..."
            @keyup.enter.exact="send"
            :disabled="sending"
            maxlength="500"
            show-word-limit
          />
          <el-button class="send-btn" type="primary" @click="send" :loading="sending" :icon="'Promotion'">发送</el-button>
        </div>
      </div>
    </Transition>
  </div>
</template>

<style scoped lang="scss">
.floating-chat {
  position: fixed;
  right: 24px;
  bottom: 24px;
  z-index: 2000;
}

.chat-toggle {
  width: 56px;
  height: 56px;
  border-radius: 50%;
  background: linear-gradient(135deg, #409EFF, #67C23A);
  color: #fff;
  display: flex;
  align-items: center;
  justify-content: center;
  cursor: pointer;
  box-shadow: 0 4px 16px rgba(64, 158, 255, 0.35);
  transition: transform 0.2s, box-shadow 0.2s;

  &:hover {
    transform: scale(1.08);
    box-shadow: 0 6px 20px rgba(64, 158, 255, 0.5);
  }
}

.chat-panel {
  position: absolute;
  right: 0;
  bottom: 68px;
  min-width: 300px;
  min-height: 350px;
  background: #fff;
  border-radius: 16px;
  box-shadow: 0 8px 32px rgba(0, 0, 0, 0.12);
  display: flex;
  flex-direction: column;
  overflow: hidden;

  &.resizing {
    user-select: none;
    transition: none;
  }
}

.resize-handle {
  position: absolute;
  top: 6px;
  left: 6px;
  width: 20px;
  height: 20px;
  cursor: nwse-resize;
  display: flex;
  align-items: center;
  justify-content: center;
  z-index: 10;
  border-radius: 4px;
  opacity: 0.5;
  transition: opacity 0.2s;

  &:hover {
    opacity: 1;
    background: rgba(255, 255, 255, 0.2);
  }
}

.chat-header {
  padding: 14px 18px;
  background: linear-gradient(135deg, #409EFF, #66b1ff);
  color: #fff;
  font-weight: 600;
  font-size: 15px;
  display: flex;
  justify-content: space-between;
  align-items: center;
  flex-shrink: 0;

  .el-button {
    color: #fff;
  }
}

.chat-body {
  flex: 1;
  overflow-y: auto;
  padding: 16px;
  display: flex;
  flex-direction: column;
  gap: 12px;
  background: #f5f7fa;
}

.chat-message {
  display: flex;
  max-width: 85%;

  &.user {
    align-self: flex-end;

    .chat-bubble {
      background: #409EFF;
      color: #fff;
      border-radius: 16px 16px 4px 16px;
    }

    .chat-time {
      color: rgba(255, 255, 255, 0.7);
    }
  }

  &.assistant {
    align-self: flex-start;

    .chat-bubble {
      background: #fff;
      color: #303133;
      border-radius: 16px 16px 16px 4px;
      box-shadow: 0 1px 4px rgba(0, 0, 0, 0.06);
    }
  }
}

.chat-bubble {
  padding: 10px 14px;
  font-size: 14px;
  line-height: 1.6;
  white-space: pre-wrap;
  word-break: break-word;

  &.typing {
    display: flex;
    gap: 5px;
    padding: 14px 18px;

    .dot {
      width: 8px;
      height: 8px;
      border-radius: 50%;
      background: #c0c4cc;
      animation: typing 1.4s infinite ease-in-out both;

      &:nth-child(1) { animation-delay: 0s; }
      &:nth-child(2) { animation-delay: 0.2s; }
      &:nth-child(3) { animation-delay: 0.4s; }
    }
  }
}

.chat-time {
  font-size: 11px;
  margin-top: 4px;
  text-align: right;
  color: #c0c4cc;
}

.chat-footer {
  padding: 12px;
  border-top: 1px solid #ebeef5;
  flex-shrink: 0;
  background: #fff;
  display: flex;
  gap: 8px;
  align-items: flex-end;
}

.send-btn {
  flex-shrink: 0;
}

.chat-slide-enter-active,
.chat-slide-leave-active {
  transition: all 0.3s ease;
}

.chat-slide-enter-from,
.chat-slide-leave-to {
  opacity: 0;
  transform: translateY(16px) scale(0.95);
}

@keyframes typing {
  0%, 80%, 100% { transform: scale(0.6); opacity: 0.4; }
  40% { transform: scale(1); opacity: 1; }
}
</style>
