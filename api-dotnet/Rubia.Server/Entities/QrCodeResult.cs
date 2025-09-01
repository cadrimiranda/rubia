namespace Rubia.Server.Entities;

public class QrCodeResult
{
    public bool Success { get; set; }
    public object? Data { get; set; }
    public string? Type { get; set; }
    public string? Error { get; set; }

    public static QrCodeResult CreateSuccess(object data, string type)
    {
        return new QrCodeResult
        {
            Success = true,
            Data = data,
            Type = type
        };
    }

    public static QrCodeResult CreateError(string error)
    {
        return new QrCodeResult
        {
            Success = false,
            Error = error
        };
    }
}