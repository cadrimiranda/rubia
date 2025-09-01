using System.ComponentModel;

namespace Rubia.Server.Enums;

/// <summary>
/// Enum para representar os tipos de revisão de template
/// </summary>
public enum RevisionType
{
    /// <summary>
    /// Revisão criada quando o template é criado pela primeira vez
    /// </summary>
    [Description("Criação")]
    Create,
    
    /// <summary>
    /// Revisão criada quando o template é editado
    /// </summary>
    [Description("Edição")]
    Edit,
    
    /// <summary>
    /// Revisão criada quando o template é excluído (soft delete)
    /// </summary>
    [Description("Exclusão")]
    Delete,
    
    /// <summary>
    /// Revisão criada quando o template é restaurado
    /// </summary>
    [Description("Restauração")]
    Restore,
    
    /// <summary>
    /// Revisão criada quando o template é melhorado por IA
    /// </summary>
    [Description("Melhoria por IA")]
    AiEnhancement
}