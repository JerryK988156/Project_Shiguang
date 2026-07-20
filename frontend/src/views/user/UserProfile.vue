<script setup>
import { onMounted, reactive, ref } from 'vue'
import { ElMessage } from 'element-plus'

import { useUserStore } from '@/stores/user'
import { updateProfileApi, updatePasswordApi, uploadAvatarApi } from '@/api/auth'
import { getAchievementListApi } from '@/api/achievement'

const userStore = useUserStore()

const profileForm = reactive({
  username: userStore.userInfo.username || '',
  nickname: userStore.userInfo.nickname || '',
  profile: userStore.userInfo.profile || ''
})

const avatarUrl = ref(userStore.userInfo.avatar || '')
const uploading = ref(false)
const achievements = ref([])

const passwordForm = reactive({
  oldPassword: '',
  newPassword: '',
  confirmPassword: ''
})

const profileLoading = ref(false)
const passwordLoading = ref(false)

const loadAchievements = async () => {
  try {
    const data = await getAchievementListApi()
    achievements.value = Array.isArray(data) ? data : []
  } catch {
    achievements.value = []
  }
}

onMounted(loadAchievements)

const handleAvatarChange = async (event) => {
  const file = event.target.files[0]
  if (!file) return

  if (file.size > 2 * 1024 * 1024) {
    ElMessage.warning('图片大小不能超过 2MB')
    return
  }

  uploading.value = true
  try {
    const result = await uploadAvatarApi(file)
    avatarUrl.value = result.avatar || ''
    userStore.updateAvatar(avatarUrl.value)
    ElMessage.success('头像已更新')
  } catch {
    // axios 拦截器已提示
  } finally {
    uploading.value = false
    event.target.value = ''
  }
}

const handleUpdateProfile = async () => {
  if (!profileForm.nickname) {
    ElMessage.warning('昵称不能为空')
    return
  }
  profileLoading.value = true
  try {
    const updated = await updateProfileApi({
      username: profileForm.username || undefined,
      nickname: profileForm.nickname,
      profile: profileForm.profile || undefined
    })
    userStore.setUserInfo(updated)
    ElMessage.success('个人资料已更新')
  } finally {
    profileLoading.value = false
  }
}

const handleUpdatePassword = async () => {
  if (!passwordForm.oldPassword || !passwordForm.newPassword) {
    ElMessage.warning('请填写旧密码和新密码')
    return
  }
  if (passwordForm.newPassword.length < 6) {
    ElMessage.warning('新密码长度不能少于 6 位')
    return
  }
  if (passwordForm.newPassword !== passwordForm.confirmPassword) {
    ElMessage.warning('两次输入的新密码不一致')
    return
  }
  passwordLoading.value = true
  try {
    await updatePasswordApi({
      oldPassword: passwordForm.oldPassword,
      newPassword: passwordForm.newPassword
    })
    ElMessage.success('密码已修改')
    passwordForm.oldPassword = ''
    passwordForm.newPassword = ''
    passwordForm.confirmPassword = ''
  } finally {
    passwordLoading.value = false
  }
}
</script>

<template>
  <div class="page-container profile-page">
    <el-card>
      <template #header><span>个人资料</span></template>
      <el-form label-width="80px" label-position="left">
        <el-form-item label="头像">
          <div class="avatar-section">
            <label class="avatar-wrapper" :class="{ uploading }">
              <img v-if="avatarUrl" :src="avatarUrl" class="avatar-img" />
              <span v-else class="avatar-placeholder">
                {{ profileForm.nickname.charAt(0) || userStore.userInfo.username?.charAt(0) || '?' }}
              </span>
              <input type="file" accept="image/*" class="avatar-input" @change="handleAvatarChange" />
              <div class="avatar-overlay" v-if="!uploading">点击上传</div>
              <div class="avatar-overlay uploading-overlay" v-else>上传中...</div>
            </label>
            <div class="avatar-tip">点击头像上传，仅支持 jpg/png/gif/webp，限 2MB</div>
          </div>
        </el-form-item>
        <el-form-item label="账号">
          <el-input v-model="profileForm.username" placeholder="修改账号（需唯一）" maxlength="50" />
        </el-form-item>
        <el-form-item label="昵称">
          <el-input v-model="profileForm.nickname" placeholder="请输入昵称" maxlength="50" />
        </el-form-item>
        <el-form-item label="简介">
          <el-input v-model="profileForm.profile" type="textarea" placeholder="个性签名 / 个人简介" maxlength="255" show-word-limit rows="3" />
        </el-form-item>
        <el-form-item>
          <el-button type="primary" :loading="profileLoading" @click="handleUpdateProfile">保存资料</el-button>
        </el-form-item>
      </el-form>
    </el-card>

    <el-card>
      <template #header><span>我的成就</span></template>
      <div v-if="achievements.length > 0" class="achievement-grid">
        <div v-for="ach in achievements" :key="ach.id" class="achievement-card">
          <div class="ach-emoji">🏆</div>
          <div class="ach-title">{{ ach.goalTitle }}</div>
          <div class="ach-days">{{ ach.badgeName }} · {{ ach.milestoneDays }} 天</div>
          <div class="ach-date">{{ ach.achievedDate }}</div>
        </div>
      </div>
      <div v-else class="empty-achievement">坚持打卡，解锁你的第一个成就！</div>
    </el-card>

    <el-card>
      <template #header><span>修改密码</span></template>
      <el-form label-width="80px" label-position="left">
        <el-form-item label="旧密码">
          <el-input v-model="passwordForm.oldPassword" type="password" show-password placeholder="请输入旧密码" />
        </el-form-item>
        <el-form-item label="新密码">
          <el-input v-model="passwordForm.newPassword" type="password" show-password placeholder="请输入新密码" />
        </el-form-item>
        <el-form-item label="确认密码">
          <el-input v-model="passwordForm.confirmPassword" type="password" show-password placeholder="请再次输入新密码" />
        </el-form-item>
        <el-form-item>
          <el-button type="primary" :loading="passwordLoading" @click="handleUpdatePassword">修改密码</el-button>
        </el-form-item>
      </el-form>
    </el-card>
  </div>
</template>

<style scoped lang="scss">
.profile-page {
  display: grid;
  gap: 20px;
  max-width: 540px;
  margin: 0 auto;
}

.avatar-section {
  display: flex;
  align-items: center;
  gap: 20px;
}

.avatar-wrapper {
  position: relative;
  width: 80px;
  height: 80px;
  border-radius: 50%;
  cursor: pointer;
  display: flex;
  align-items: center;
  justify-content: center;
  overflow: hidden;
  background: #e5e7eb;
  transition: opacity 0.2s;

  &:hover .avatar-overlay { opacity: 1; }
  &.uploading { opacity: 0.6; pointer-events: none; }
}

.avatar-img {
  width: 100%;
  height: 100%;
  object-fit: cover;
  border-radius: 50%;
}

.avatar-placeholder {
  font-size: 32px;
  font-weight: 700;
  color: #6b7280;
}

.avatar-input {
  position: absolute;
  inset: 0;
  opacity: 0;
  cursor: pointer;
}

.avatar-overlay {
  position: absolute;
  inset: 0;
  display: flex;
  align-items: center;
  justify-content: center;
  background: rgba(0, 0, 0, 0.5);
  color: #fff;
  font-size: 12px;
  border-radius: 50%;
  opacity: 0;
  transition: opacity 0.2s;
}

.avatar-tip { color: #9ca3af; font-size: 13px; }

.achievement-grid {
  display: grid;
  grid-template-columns: repeat(3, 1fr);
  gap: 12px;
}

.achievement-card {
  text-align: center;
  padding: 12px 8px;
  border-radius: 10px;
  background: #f9fafb;
  border: 1px solid #e5e7eb;
}

.ach-emoji { font-size: 32px; }
.ach-title { font-size: 12px; color: #374151; margin: 6px 0 4px; }
.ach-days { font-size: 12px; font-weight: 600; color: #111827; }
.ach-date { font-size: 11px; color: #9ca3af; margin-top: 4px; }

.empty-achievement {
  text-align: center;
  padding: 32px 0;
  color: #9ca3af;
  font-size: 14px;
}
</style>
