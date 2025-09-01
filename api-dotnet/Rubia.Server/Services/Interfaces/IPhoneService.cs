namespace Rubia.Server.Services.Interfaces;

public interface IPhoneService
{
    string Normalize(string phone);
    bool IsValid(string phone);
}