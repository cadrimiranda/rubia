// Export all adapters
export { conversationAdapter } from './conversationAdapter'
export { messageAdapter } from './messageAdapter'
export { customerAdapter } from './customerAdapter'

// Re-export for convenience
import { conversationAdapter } from './conversationAdapter'
import { messageAdapter } from './messageAdapter'
import { customerAdapter } from './customerAdapter'

export const conversations = conversationAdapter
export const messages = messageAdapter
export const customers = customerAdapter

export default {
  conversations: conversationAdapter,
  messages: messageAdapter,
  customers: customerAdapter
}