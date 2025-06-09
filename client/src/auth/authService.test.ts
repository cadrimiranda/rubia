import { describe, it, expect, beforeEach, vi, afterEach } from 'vitest'
import { authService } from './authService'
import { apiClient } from '../api/client'
import type { LoginRequest, LoginResponse } from '../api/types'

vi.mock('../api/client')
vi.mock('../utils/company', () => ({
  getCompanyFromSubdomain: () => ({ slug: 'test-company' }),
  getCurrentCompanySlug: () => 'test-company'
}))

const mockApiClient = vi.mocked(apiClient)

describe('AuthService', () => {
  const mockLoginResponse: LoginResponse = {
    token: 'jwt-token-123',
    user: {
      id: '123',
      name: 'Test User',
      email: 'test@example.com',
      role: 'AGENT',
      departmentId: 'dept-1',
      department: {
        id: 'dept-1',
        name: 'Support',
        description: 'Support Department',
        autoAssign: true,
        createdAt: '2024-01-01T00:00:00Z',
        updatedAt: '2024-01-01T00:00:00Z'
      },
      avatarUrl: 'https://example.com/avatar.png',
      isOnline: true,
      companyId: 'company-123',
      createdAt: '2024-01-01T00:00:00Z',
      updatedAt: '2024-01-01T00:00:00Z'
    },
    expiresIn: 3600,
    companyId: 'company-123',
    companySlug: 'test-company'
  }

  const mockCredentials: LoginRequest = {
    email: 'test@example.com',
    password: 'password123'
  }

  beforeEach(() => {
    vi.clearAllMocks()
    localStorage.clear()
    vi.spyOn(Date, 'now').mockReturnValue(1000000)
  })

  afterEach(() => {
    vi.restoreAllMocks()
  })

  describe('login', () => {
    it('should login successfully with valid credentials', async () => {
      mockApiClient.post.mockResolvedValueOnce(mockLoginResponse)

      const result = await authService.login(mockCredentials)

      expect(mockApiClient.post).toHaveBeenCalledWith('/api/auth/login', {
        email: 'test@example.com',
        password: 'password123',
        companySlug: 'test-company'
      })

      expect(result).toEqual({
        id: '123',
        name: 'Test User',
        email: 'test@example.com',
        role: 'AGENT',
        department: {
          id: 'dept-1',
          name: 'Support'
        },
        avatarUrl: 'https://example.com/avatar.png',
        isOnline: true,
        companyId: 'company-123',
        companySlug: 'test-company'
      })

      expect(localStorage.getItem('auth_token')).toBe('jwt-token-123')
      expect(localStorage.getItem('token_expires_at')).toBe('3601000')
    })

    it('should throw error when login fails', async () => {
      const error = new Error('Invalid credentials')
      mockApiClient.post.mockRejectedValueOnce(error)

      await expect(authService.login(mockCredentials)).rejects.toThrow('Invalid credentials')

      expect(localStorage.getItem('auth_token')).toBeNull()
      expect(localStorage.getItem('auth_user')).toBeNull()
    })

    it('should include company context in login request', async () => {
      mockApiClient.post.mockResolvedValueOnce(mockLoginResponse)

      await authService.login(mockCredentials)

      expect(mockApiClient.post).toHaveBeenCalledWith('/api/auth/login', 
        expect.objectContaining({
          companySlug: 'test-company'
        })
      )
    })
  })

  describe('logout', () => {
    beforeEach(() => {
      localStorage.setItem('auth_token', 'jwt-token-123')
      localStorage.setItem('auth_user', JSON.stringify(mockLoginResponse.user))
    })

    it('should logout successfully and clear local data', async () => {
      mockApiClient.post.mockResolvedValueOnce({})

      await authService.logout()

      expect(mockApiClient.post).toHaveBeenCalledWith('/api/auth/logout')
      expect(localStorage.getItem('auth_token')).toBeNull()
      expect(localStorage.getItem('auth_user')).toBeNull()
      expect(localStorage.getItem('auth_company')).toBeNull()
    })

    it('should clear local data even if server logout fails', async () => {
      mockApiClient.post.mockRejectedValueOnce(new Error('Server error'))

      await authService.logout()

      expect(localStorage.getItem('auth_token')).toBeNull()
      expect(localStorage.getItem('auth_user')).toBeNull()
    })
  })

  describe('isAuthenticated', () => {
    it('should return true when user is authenticated with valid token', () => {
      const futureTimestamp = Date.now() + 3600000
      localStorage.setItem('auth_token', 'jwt-token-123')
      localStorage.setItem('token_expires_at', futureTimestamp.toString())
      localStorage.setItem('auth_user', JSON.stringify({
        ...mockLoginResponse.user,
        companySlug: 'test-company'
      }))

      expect(authService.isAuthenticated()).toBe(true)
    })

    it('should return false when token is expired', () => {
      const pastTimestamp = Date.now() - 3600000
      localStorage.setItem('auth_token', 'jwt-token-123')
      localStorage.setItem('token_expires_at', pastTimestamp.toString())
      localStorage.setItem('auth_user', JSON.stringify(mockLoginResponse.user))

      expect(authService.isAuthenticated()).toBe(false)
    })

    it('should return false when user is in wrong company', () => {
      const futureTimestamp = Date.now() + 3600000
      localStorage.setItem('auth_token', 'jwt-token-123')
      localStorage.setItem('token_expires_at', futureTimestamp.toString())
      localStorage.setItem('auth_user', JSON.stringify({
        ...mockLoginResponse.user,
        companySlug: 'different-company'
      }))

      expect(authService.isAuthenticated()).toBe(false)
    })

    it('should return false when no token exists', () => {
      expect(authService.isAuthenticated()).toBe(false)
    })
  })

  describe('refreshToken', () => {
    it('should refresh token successfully', async () => {
      localStorage.setItem('refresh_token', 'refresh-token-123')
      mockApiClient.post.mockResolvedValueOnce(mockLoginResponse)

      const newToken = await authService.refreshToken()

      expect(mockApiClient.post).toHaveBeenCalledWith('/api/auth/refresh', {
        refreshToken: 'refresh-token-123'
      })
      expect(newToken).toBe('jwt-token-123')
      expect(localStorage.getItem('auth_token')).toBe('jwt-token-123')
    })

    it('should throw error when no refresh token exists', async () => {
      await expect(authService.refreshToken()).rejects.toThrow('Não há refresh token disponível')
    })

    it('should clear auth data when refresh fails', async () => {
      localStorage.setItem('refresh_token', 'invalid-token')
      localStorage.setItem('auth_token', 'old-token')
      mockApiClient.post.mockRejectedValueOnce(new Error('Invalid refresh token'))

      await expect(authService.refreshToken()).rejects.toThrow('Invalid refresh token')

      expect(localStorage.getItem('auth_token')).toBeNull()
      expect(localStorage.getItem('refresh_token')).toBeNull()
    })
  })

  describe('hasPermission', () => {
    beforeEach(() => {
      localStorage.setItem('auth_user', JSON.stringify({
        ...mockLoginResponse.user,
        role: 'SUPERVISOR'
      }))
    })

    it('should return true for equal or lower permission levels', () => {
      expect(authService.hasPermission('AGENT')).toBe(true)
      expect(authService.hasPermission('SUPERVISOR')).toBe(true)
    })

    it('should return false for higher permission levels', () => {
      expect(authService.hasPermission('ADMIN')).toBe(false)
    })

    it('should return false when no user is authenticated', () => {
      localStorage.clear()
      expect(authService.hasPermission('AGENT')).toBe(false)
    })
  })

  describe('updateOnlineStatus', () => {
    beforeEach(() => {
      localStorage.setItem('auth_user', JSON.stringify(mockLoginResponse.user))
    })

    it('should update online status successfully', async () => {
      mockApiClient.put.mockResolvedValueOnce({})

      await authService.updateOnlineStatus(false)

      expect(mockApiClient.put).toHaveBeenCalledWith('/api/users/123/status', {
        isOnline: false
      })

      const updatedUser = JSON.parse(localStorage.getItem('auth_user') || '{}')
      expect(updatedUser.isOnline).toBe(false)
    })

    it('should handle error gracefully', async () => {
      mockApiClient.put.mockRejectedValueOnce(new Error('Server error'))

      await expect(authService.updateOnlineStatus(true)).resolves.toBeUndefined()
    })
  })

  describe('isTokenExpiringSoon', () => {
    it('should return true when token expires in less than 10 minutes', () => {
      const soonTimestamp = Date.now() + (5 * 60 * 1000) // 5 minutes
      localStorage.setItem('token_expires_at', soonTimestamp.toString())

      expect(authService.isTokenExpiringSoon()).toBe(true)
    })

    it('should return false when token expires in more than 10 minutes', () => {
      const laterTimestamp = Date.now() + (15 * 60 * 1000) // 15 minutes
      localStorage.setItem('token_expires_at', laterTimestamp.toString())

      expect(authService.isTokenExpiringSoon()).toBe(false)
    })

    it('should return false when no expiration time exists', () => {
      expect(authService.isTokenExpiringSoon()).toBe(false)
    })
  })

  describe('hasCompanyAccess', () => {
    it('should return true when user belongs to current company', () => {
      localStorage.setItem('auth_user', JSON.stringify({
        ...mockLoginResponse.user,
        companySlug: 'test-company'
      }))

      expect(authService.hasCompanyAccess()).toBe(true)
    })

    it('should return false when user belongs to different company', () => {
      localStorage.setItem('auth_user', JSON.stringify({
        ...mockLoginResponse.user,
        companySlug: 'different-company'
      }))

      expect(authService.hasCompanyAccess()).toBe(false)
    })

    it('should return false when no user is authenticated', () => {
      expect(authService.hasCompanyAccess()).toBe(false)
    })
  })
})