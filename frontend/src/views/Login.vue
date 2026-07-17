<script setup>
import { reactive, ref } from 'vue'
import { useRouter } from 'vue-router'
import { ElMessage } from 'element-plus'

import { useUserStore } from '@/stores/user'
import { getCurrentUserApi, loginApi } from '@/api/auth'

const router = useRouter()
const userStore = useUserStore()
const loading = ref(false)

const form = reactive({
  username: 'admin',
  password: '123456'
})

const handleLogin = async () => {
  if (!form.username || !form.password) {
    ElMessage.warning('请输入账号和密码')
    return
  }

  loading.value = true
  try {
    const loginData = await loginApi(form)
    userStore.setToken(loginData?.token || `${form.username}-session`)
    const userInfo = await getCurrentUserApi()
    userStore.setUserInfo(userInfo)
    ElMessage.success('登录成功')
    router.push(userStore.isAdmin() ? '/admin/dashboard' : '/dashboard')
  } finally {
    loading.value = false
  }
}
</script>

<template>
  <div class="login-page">
    <div class="login-card">
      <div class="title">拾光计划</div>
      <div class="subtitle">学习打卡与目标管理系统</div>

      <el-form label-position="top">
        <el-form-item label="账号">
          <el-input v-model="form.username" placeholder="请输入账号" />
        </el-form-item>
        <el-form-item label="密码">
          <el-input v-model="form.password" type="password" show-password placeholder="请输入密码" />
        </el-form-item>
        <el-button :loading="loading" type="primary" class="submit-btn" @click="handleLogin">进入系统</el-button>
      </el-form>
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
</style>
