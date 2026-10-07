using System.ComponentModel.DataAnnotations;
using System.Text;
using Microsoft.IdentityModel.Tokens;

namespace HexMaster.LeakOut.Players.Services;

/// <summary>Settings of the access tokens the server issues to signed-in players.</summary>
public sealed class AccessTokenOptions : IValidatableObject
{
    public const string SectionName = "Authentication";
    public const int MinimumSigningKeyBytes = 32;

    [Required]
    public string Issuer { get; set; } = "leak-out-server";

    [Required]
    public string Audience { get; set; } = "leak-out-game";

    /// <summary>The HS256 signing key; at least <see cref="MinimumSigningKeyBytes"/> bytes as UTF-8.</summary>
    [Required]
    public string SigningKey { get; set; } = string.Empty;

    [Range(typeof(TimeSpan), "00:01:00", "1.00:00:00")]
    public TimeSpan Lifetime { get; set; } = TimeSpan.FromHours(1);

    public SymmetricSecurityKey CreateSigningKey() => new(Encoding.UTF8.GetBytes(SigningKey));

    public IEnumerable<ValidationResult> Validate(ValidationContext validationContext)
    {
        if (Encoding.UTF8.GetByteCount(SigningKey) < MinimumSigningKeyBytes)
        {
            yield return new ValidationResult(
                $"{SectionName}:{nameof(SigningKey)} must be at least {MinimumSigningKeyBytes} bytes.",
                [nameof(SigningKey)]);
        }
    }
}
