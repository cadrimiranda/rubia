using System.Text.RegularExpressions;
using Rubia.Server.Services.Interfaces;

namespace Rubia.Server.Services;

public class PhoneService : IPhoneService
{
    private static readonly Regex BrazilianPhoneRegex = new(@"^\+55\d{10,11}$", RegexOptions.Compiled);
    private static readonly Regex CleanupRegex = new(@"[^\d+]", RegexOptions.Compiled);

    public string Normalize(string phone)
    {
        if (string.IsNullOrEmpty(phone))
        {
            return phone;
        }

        // Remove espaços, hífens, parênteses, etc.
        var cleaned = CleanupRegex.Replace(phone, "");

        // Se não começar com +55, adiciona
        if (!cleaned.StartsWith("+55"))
        {
            // Se começar com 55, adiciona o +
            if (cleaned.StartsWith("55"))
            {
                cleaned = "+" + cleaned;
            }
            // Se começar com 0, remove e adiciona +55
            else if (cleaned.StartsWith("0"))
            {
                cleaned = "+55" + cleaned.Substring(1);
            }
            // Se não começar com nenhum dos acima, assume que é um número local e adiciona +55
            else
            {
                cleaned = "+55" + cleaned;
            }
        }

        return cleaned;
    }

    public bool IsValid(string phone)
    {
        if (string.IsNullOrEmpty(phone))
        {
            return false;
        }

        var normalized = Normalize(phone);
        return BrazilianPhoneRegex.IsMatch(normalized);
    }
}