import { createRouter, createWebHistory } from 'vue-router'
import { ElMessage } from 'element-plus'

import Layout from '@/views/Layout.vue'
import Login from '@/views/Login.vue'
import Dashboard from '@/views/dashboard/Index.vue'
import pinia from '@/stores'
import { useUserStore } from '@/stores/user'
import { getCurrentUserApi } from '@/api/auth'

import GoalList from '@/views/goal/GoalList.vue'
import GoalEdit from '@/views/goal/GoalEdit.vue'
import CheckinAdd from '@/views/checkin/CheckinAdd.vue'
import CheckinList from '@/views/checkin/CheckinList.vue'
import PersonalStat from '@/views/stat/PersonalStat.vue'
import AdminStat from '@/views/admin/AdminStat.vue'
import AdminDashboard from '@/views/admin/AdminDashboard.vue'
import UserProfile from '@/views/user/UserProfile.vue'

const routes = [
  {
    path: '/login',
    name: 'login',
    component: Login,
    meta: { requiresAuth: false }
  },
  {
    path: '/',
    component: Layout,
    redirect: '/dashboard',
    meta: { requiresAuth: true },
    children: [
      {
        path: '/dashboard',
        name: 'dashboard',
        component: Dashboard,
        meta: { title: '首页看板' }
      },
      {
        path: '/goal/list',
        name: 'goal-list',
        component: GoalList,
        meta: { title: '目标列表' }
      },
      {
        path: '/goal/edit',
        name: 'goal-add',
        component: GoalEdit,
        meta: { title: '新建目标' }
      },
      {
        path: '/goal/edit/:id',
        name: 'goal-edit',
        component: GoalEdit,
        meta: { title: '编辑目标' }
      },
      {
        path: '/checkin/add',
        name: 'checkin-add',
        component: CheckinAdd,
        meta: { title: '今日打卡' }
      },
      {
        path: '/checkin/list',
        name: 'checkin-list',
        component: CheckinList,
        meta: { title: '打卡记录' }
      },
      {
        path: '/stat/personal',
        name: 'stat-personal',
        component: PersonalStat,
        meta: { title: '个人统计' }
      },
      {
        path: '/admin/dashboard',
        name: 'admin-dashboard',
        component: AdminDashboard,
        meta: { title: '首页看板', requiresAdmin: true }
      },
      {
        path: '/admin/stat',
        name: 'admin-stat',
        component: AdminStat,
        meta: { title: '管理概览', requiresAdmin: true }
      },
      {
        path: '/user/profile',
        name: 'user-profile',
        component: UserProfile,
        meta: { title: '个人中心' }
      }
    ]
  }
]

const router = createRouter({
  history: createWebHistory(),
  routes
})

router.beforeEach(async (to) => {
  const userStore = useUserStore(pinia)
  const token = localStorage.getItem('shiguang-token')

  if (to.meta.requiresAuth && !token) {
    return '/login'
  }

  if (to.path === '/login' && token) {
    const userStore = useUserStore(pinia)
    return userStore.isAdmin() ? '/admin/dashboard' : '/dashboard'
  }

  if (to.meta.requiresAuth && token && !userStore.hasUserInfo()) {
    try {
      const userInfo = await getCurrentUserApi()
      userStore.setUserInfo(userInfo)
    } catch (error) {
      userStore.resetUser()
      return '/login'
    }
  }

  if (to.meta.requiresAdmin && !userStore.isAdmin()) {
    ElMessage.warning('当前账号无管理员权限')
    return '/dashboard'
  }

  return true
})

export default router
