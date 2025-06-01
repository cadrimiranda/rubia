import { Search } from 'lucide-react'
import { useChatStore } from '../../store/useChatStore'

const SearchBar = () => {
  const { searchQuery, setSearchQuery } = useChatStore()

  return (
    <div className="relative">
      <Search 
        size={16} 
        className="absolute left-3 top-1/2 transform -translate-y-1/2 text-neutral-400" 
      />
      <input
        type="text"
        placeholder="Buscar por nome ou telefone"
        value={searchQuery}
        onChange={(e) => setSearchQuery(e.target.value)}
        className="w-full h-9 pl-10 pr-4 bg-neutral-100 border-0 rounded-lg text-sm text-neutral-700 placeholder:text-neutral-500 focus:outline-none focus:ring-2 focus:ring-ruby-500/20 focus:bg-white transition-all duration-200"
      />
    </div>
  )
}

export default SearchBar