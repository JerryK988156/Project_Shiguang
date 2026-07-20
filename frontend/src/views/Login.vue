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
    userStore.setToken(loginData?.token || '')
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
    userStore.setToken(userData?.token || '')
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
    <div class="login-card">
      <div class="title">拾光计划</div>
      <div class="subtitle">{{ isRegister ? '创建新账号' : '学习打卡与目标管理系统' }}</div>

      <el-form label-position="top">
        <el-form-item label="账号">
          <el-input v-model="form.username" placeholder="请输入账号（4-20位字母/数字/下划线）" />
        </el-form-item>
        <el-form-item label="密码">
          <el-input v-model="form.password" type="password" show-password placeholder="请输入密码（至少6位）" />
        </el-form-item>
        <el-form-item v-if="isRegister" label="昵称（可选）">
          <el-input v-model="form.nickname" placeholder="给自己取个名字" />
        </el-form-item>
        <el-button
          v-if="!isRegister"
          :loading="loading"
          type="primary"
          class="submit-btn"
          @click="handleLogin"
        >进入系统</el-button>
        <el-button
          v-else
          :loading="loading"
          type="primary"
          class="submit-btn"
          @click="handleRegister"
        >注册</el-button>
      </el-form>

      <div class="switch-link">
        <el-button link type="primary" @click="switchMode">
          {{ isRegister ? '已有账号？去登录' : '没有账号？立即注册' }}
        </el-button>
      </div>
    </div>
  </div>
</template>

<style scoped lang="scss">
.login-page {
  min-height: 100vh;
  display: flex;
  align-items: center;
  justify-content: center;
  background: linear-gradient(135deg, #eef2ff 0%, #dbeafe 100%);
}

.login-card {
  width: 420px;
  padding: 32px;
  background: #fff;
  border-radius: 20px;
  box-shadow: 0 20px 60px rgba(15, 23, 42, 0.12);
}

.title {
  font-size: 28px;
  font-weight: 700;
  color: #111827;
}

.subtitle {
  margin: 8px 0 24px;
  color: #6b7280;
}

.submit-btn {
  width: 100%;
}

.switch-link {
  text-align: center;
  margin-top: 16px;
}
</style>
