/**
 * Company detection utilities for multi-tenant architecture
 */

export interface CompanyInfo {
  slug: string;
  subdomain: string;
  isLocalDevelopment: boolean;
}

/**
 * Extract company slug from subdomain
 * Examples:
 * - company1.rubia.com -> company1
 * - localhost:3000 -> default (development)
 * - rubia.com -> default (main domain)
 */
export const getCompanyFromSubdomain = (): CompanyInfo => {
  const hostname = window.location.hostname;
  const parts = hostname.split('.');
  
  // Extract subdomain (first part)
  // Examples:
  // - rubia.localhost -> rubia
  // - company1.rubia.com -> company1
  // - localhost -> localhost (special case for development)
  const companySlug = parts[0];
  const isLocalDevelopment = hostname.includes('localhost') || hostname === '127.0.0.1';
  
  return {
    slug: companySlug,
    subdomain: hostname,
    isLocalDevelopment
  };
};

/**
 * Get the current company slug for API calls
 */
export const getCurrentCompanySlug = (): string => {
  return getCompanyFromSubdomain().slug;
};

/**
 * Build company-specific URL for redirects
 */
export const buildCompanyUrl = (companySlug: string, path: string = ''): string => {
  const { isLocalDevelopment } = getCompanyFromSubdomain();
  
  if (isLocalDevelopment) {
    const baseUrl = `${window.location.protocol}//${window.location.host}`;
    return `${baseUrl}${path}${path.includes('?') ? '&' : '?'}company=${companySlug}`;
  }
  
  // Production: use subdomain
  const protocol = window.location.protocol;
  const domain = window.location.hostname.split('.').slice(-2).join('.'); // Extract main domain
  
  return `${protocol}//${companySlug}.${domain}${path}`;
};

/**
 * Validate if current company context is valid
 */
export const isValidCompanyContext = (companySlug?: string): boolean => {
  const currentCompany = getCurrentCompanySlug();
  
  if (!companySlug) {
    return currentCompany !== '';
  }
  
  return currentCompany === companySlug;
};