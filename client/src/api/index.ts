// Export all API services
export { default as apiClient } from './client'
export { conversationApi } from './services/conversationApi'
export { messageApi } from './services/messageApi'
export { customerApi } from './services/customerApi'
export { userApi } from './services/userApi'
export { departmentApi } from './services/departmentApi'

// Export types
export type * from './types'

// Re-export for convenience
export {
  ConversationAPI,
  conversationApi as conversations
} from './services/conversationApi'

export {
  MessageAPI,
  messageApi as messages
} from './services/messageApi'

export {
  CustomerAPI,
  customerApi as customers
} from './services/customerApi'

export {
  UserAPI,
  userApi as users
} from './services/userApi'

export {
  DepartmentAPI,
  departmentApi as departments
} from './services/departmentApi'