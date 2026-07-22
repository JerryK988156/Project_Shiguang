<script setup>
import { reactive, ref } from 'vue'
import { useRouter } from 'vue-router'
import { ElMessage } from 'element-plus'

import { useUserStore } from '@/stores/user'
import { getCurrentUserApi, loginApi, registerApi } from '@/api/auth'

const router = useRouter()
const userStore = useUserStore()
const loading = ref(false)
const isRegister = ref(false)

const form = reactive({
  username: 'admin',
  password: '123456',
  nickname: ''
})

const switchMode = () => {
  isRegister.value = !isRegister.value
  form.username = isRegister.value ? '' : 'admin'
  form.password = isRegister.value ? '' : '123456'
  form.nickname = ''
}

const handleLogin = async () => {
  if (!form.username || !form.password) {
    ElMessage.warning('请输入账号和密码')
    return
  }
  loading.value = true
  try {
    const loginData = await loginApi(form)
    userStore.setToken(loginData?.accessToken || '')
    if (loginData?.refreshToken) {
      localStorage.setItem('shiguang-refresh-token', loginData.refreshToken)
    }
    const userInfo = await getCurrentUserApi()
    userStore.setUserInfo(userInfo)
    ElMessage.success('登录成功')
    router.push(userStore.isAdmin() ? '/admin/dashboard' : '/dashboard')
  } finally {
    loading.value = false
  }
}

const handleRegister = async () => {
  if (!form.username || !form.password) {
    ElMessage.warning('请输入账号和密码')
    return
  }
  if (form.username.length < 4 || form.username.length > 20) {
    ElMessage.warning('账号长度需在 4-20 位之间')
    return
  }
  if (form.password.length < 6) {
    ElMessage.warning('密码长度不能少于 6 位')
    return
  }
  loading.value = true
  try {
    const userData = await registerApi({
      username: form.username,
      password: form.password,
      nickname: form.nickname || undefined
    })
    userStore.setToken(userData?.accessToken || '')
    if (userData?.refreshToken) {
      localStorage.setItem('shiguang-refresh-token', userData.refreshToken)
    }
    userStore.setUserInfo(userData)
    ElMessage.success('注册成功')
    router.push('/dashboard')
  } finally {
    loading.value = false
  }
}
</script>

<template>
  <div class="login-page">
    <!-- 装饰背景 -->
    <div class="bg-decoration">
      <div class="circle c1"></div>
      <div class="circle c2"></div>
      <div class="circle c3"></div>
      <div class="shape s1"></div>
      <div class="shape s2"></div>
    </div>

    <div class="login-card">
      <div class="brand">
        <div class="brand-icon">
          <svg viewBox="0 0 48 48" fill="none" xmlns="http://www.w3.org/2000/svg">
            <rect width="48" height="48" rx="12" fill="url(#g)"/>
            <path d="M24 12L28 20H36L30 26L32 34L24 28L16 34L18 26L12 20H20L24 12Z" fill="white" opacity="0.9"/>
            <defs><linearGradient id="g" x1="0" y1="0" x2="48" y2="48"><stop stop-color="#6366f1"/><stop offset="1" stop-color="#818cf8"/></linearGradient></defs>
          </svg>
        </div>
        <div class="brand-name">拾光计划</div>
        <div class="brand-desc">学习打卡与目标管理系统</div>
      </div>

      <el-form label-position="top" class="login-form">
        <el-form-item label="账号">
          <el-input
            v-model="form.username"
            placeholder="请输入账号"
            :prefix-icon="'User'"
            size="large"
          />
        </el-form-item>
        <el-form-item label="密码">
          <el-input
            v-model="form.password"
            type="password"
            show-password
            placeholder="请输入密码"
            :prefix-icon="'Lock'"
            size="large"
          />
        </el-form-item>
        <el-form-item v-if="isRegister" label="昵称（可选）">
          <el-input
            v-model="form.nickname"
            placeholder="给自己取个名字"
            :prefix-icon="'Edit'"
            size="large"
          />
        </el-form-item>

        <el-button
          v-if="!isRegister"
          :loading="loading"
          type="primary"
          size="large"
          class="submit-btn"
          @click="handleLogin"
        >进入系统</el-button>
        <el-button
          v-else
          :loading="loading"
          type="primary"
          size="large"
          class="submit-btn"
          @click="handleRegister"
        >创建账号</el-button>
      </el-form>

      <div class="switch-link">
        <el-button class="switch-btn" @click="switchMode">
          {{ isRegister ? '已有账号？去登录' : '没有账号？立即注册' }}
        </el-button>
      </div>
    </div>

    <div class="login-footer">拾光计划 v3.0 · 让每一份努力都有迹可循</div>
  </div>
</template>

<style scoped lang="scss">
.login-page {
  min-height: 100vh;
  display: flex;
  flex-direction: column;
  align-items: center;
  justify-content: center;
  background: linear-gradient(135deg, #eef2ff 0%, #e0e7ff 30%, #f0fdf4 70%, #fef3c7 100%);
  position: relative;
  overflow: hidden;
}

// 装饰几何元素
.bg-decoration {
  position: absolute;
  inset: 0;
  pointer-events: none;
  
  .circle {
    position: absolute;
    border-radius: 50%;
    opacity: 0.15;
  }
  
  .c1 {
    width: 400px; height: 400px;
    background: radial-gradient(circle, #6366f1, transparent 70%);
    top: -100px; right: -100px;
  }
  
  .c2 {
    width: 300px; height: 300px;
    background: radial-gradient(circle, #10b981, transparent 70%);
    bottom: -50px; left: -80px;
  }
  
  .c3 {
    width: 200px; height: 200px;
    background: radial-gradient(circle, #f59e0b, transparent 70%);
    top: 40%; left: 10%;
  }
  
  .shape {
    position: absolute;
    border-radius: 20px;
    opacity: 0.08;
    background: #6366f1;
    transform: rotate(45deg);
  }
  
  .s1 {
    width: 120px; height: 120px;
    top: 15%; right: 20%;
  }
  
  .s2 {
    width: 80px; height: 80px;
    bottom: 20%; right: 15%;
  }
}

.login-card {
  position: relative;
  z-index: 1;
  width: 440px;
  padding: 40px 36px 32px;
  background: rgba(255, 255, 255, 0.85);
  backdrop-filter: blur(20px);
  border-radius: 24px;
  border: 1px solid rgba(255, 255, 255, 0.6);
  box-shadow: 0 25px 50px -12px rgba(0, 0, 0, 0.15), 0 0 0 1px rgba(99, 102, 241, 0.05);
}

.brand {
  text-align: center;
  margin-bottom: 32px;
  
  .brand-icon {
    width: 56px;
    height: 56px;
    margin: 0 auto 16px;
    
    svg {
      width: 100%;
      height: 100%;
    }
  }
  
  .brand-name {
    font-size: 28px;
    font-weight: 800;
    background: linear-gradient(135deg, #6366f1, #818cf8);
    -webkit-background-clip: text;
    -webkit-text-fill-color: transparent;
    background-clip: text;
    letter-spacing: 2px;
  }
  
  .brand-desc {
    margin-top: 8px;
    font-size: 14px;
    color: #64748b;
  }
}

.login-form {
  :deep(.el-form-item__label) {
    font-weight: 500;
    color: #334155;
    padding-bottom: 4px;
  }
}

.submit-btn {
  width: 100%;
  margin-top: 8px;
  height: 44px;
  font-size: 16px;
  letter-spacing: 1px;
}

.switch-link {
  text-align: center;
  margin-top: 20px;
  padding-top: 16px;
  border-top: 1px solid #e2e8f0;
}

.switch-btn {
  width: 100%;
  height: 44px;
  font-size: 15px;
  font-weight: 500;
  color: #475569 !important;
  background: #f1f5f9 !important;
  border: 1px solid #e2e8f0 !important;
  border-radius: var(--radius-sm) !important;
  transition: all 0.3s ease;
  
  &:hover {
    color: #6366f1 !important;
    background: #eef2ff !important;
    border-color: #c7d2fe !important;
  }
}

.login-footer {
  position: relative;
  z-index: 1;
  margin-top: 32px;
  font-size: 12px;
  color: #94a3b8;
}
</style>
