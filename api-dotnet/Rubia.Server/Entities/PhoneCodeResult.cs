namespace Rubia.Server.Entities;

public class PhoneCodeResult
{
    public bool Success { get; set; }
    public string? Code { get; set; }
    public string? Phone { get; set; }
    public string? Error { get; set; }
    public DateTime? ExpiresAt { get; set; }
    public Dictionary<string, object>? RawResponse { get; set; }

    public static PhoneCodeResult CreateSuccess(string code, string phone, DateTime? expiresAt = null)
    {
        return new PhoneCodeResult
        {
            Success = true,
            Code = code,
            Phone = phone,
            ExpiresAt = expiresAt
        };
    }

    public static PhoneCodeResult CreateError(string error)
    {
        return new PhoneCodeResult
        {
            Success = false,
            Error = error
        };
    }
}