namespace Rubia.Server.Entities;

public class ZApiStatus
{
    public bool Connected { get; set; }
    public string? Session { get; set; }
    public bool SmartphoneConnected { get; set; }
    public bool NeedsQrCode { get; set; }
    public string? Error { get; set; }
    public Dictionary<string, object>? RawResponse { get; set; }

    public static ZApiStatus CreateError(string error)
    {
        return new ZApiStatus
        {
            Connected = false,
            NeedsQrCode = true,
            Error = error
        };
    }
}