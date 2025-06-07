// Export all adapters
export { conversationAdapter } from './conversationAdapter'
export { messageAdapter } from './messageAdapter'
export { customerAdapter } from './customerAdapter'
export { tagAdapter } from './tagAdapter'

// Re-export for convenience
import { conversationAdapter } from './conversationAdapter'
import { messageAdapter } from './messageAdapter'
import { customerAdapter } from './customerAdapter'
import { tagAdapter } from './tagAdapter'

export const conversations = conversationAdapter
export const messages = messageAdapter
export const customers = customerAdapter
export const tags = tagAdapter

export default {
  conversations: conversationAdapter,
  messages: messageAdapter,
  customers: customerAdapter,
  tags: tagAdapter
}