using Microsoft.EntityFrameworkCore;
using Rubia.Server.Data;
using Rubia.Server.Entities;
using Rubia.Server.Enums;

namespace Rubia.Server.Repositories;

public interface IAudioMessageRepository
{
    Task<AudioMessage?> GetByIdAsync(Guid id);
    Task<AudioMessage?> GetByMessageIdAsync(string messageId);
    Task<IEnumerable<AudioMessage>> GetAllAsync(int page, int size);
    Task<IEnumerable<AudioMessage>> GetByStatusAsync(ProcessingStatus status);
    Task<IEnumerable<AudioMessage>> GetByFromNumberAsync(string fromNumber);
    Task<AudioMessage> CreateAsync(AudioMessage audioMessage);
    Task<AudioMessage> UpdateAsync(AudioMessage audioMessage);
    Task<bool> DeleteAsync(Guid id);
    Task<bool> ExistsByMessageIdAsync(string messageId);
    Task<long> CountAsync();
    Task<long> CountByStatusAsync(ProcessingStatus status);
}

public class AudioMessageRepository : IAudioMessageRepository
{
    private readonly RubiaDbContext _context;

    public AudioMessageRepository(RubiaDbContext context)
    {
        _context = context;
    }

    public async Task<AudioMessage?> GetByIdAsync(Guid id)
    {
        return await _context.AudioMessages
            .Include(a => a.Conversation)
            .FirstOrDefaultAsync(a => a.Id == id);
    }

    public async Task<AudioMessage?> GetByMessageIdAsync(string messageId)
    {
        return await _context.AudioMessages
            .Include(a => a.Conversation)
            .FirstOrDefaultAsync(a => a.MessageId == messageId);
    }

    public async Task<IEnumerable<AudioMessage>> GetAllAsync(int page, int size)
    {
        return await _context.AudioMessages
            .Include(a => a.Conversation)
            .OrderByDescending(a => a.CreatedAt)
            .Skip(page * size)
            .Take(size)
            .ToListAsync();
    }

    public async Task<IEnumerable<AudioMessage>> GetByStatusAsync(ProcessingStatus status)
    {
        return await _context.AudioMessages
            .Include(a => a.Conversation)
            .Where(a => a.Status == status)
            .OrderByDescending(a => a.CreatedAt)
            .ToListAsync();
    }

    public async Task<IEnumerable<AudioMessage>> GetByFromNumberAsync(string fromNumber)
    {
        return await _context.AudioMessages
            .Include(a => a.Conversation)
            .Where(a => a.FromNumber == fromNumber)
            .OrderByDescending(a => a.CreatedAt)
            .ToListAsync();
    }

    public async Task<AudioMessage> CreateAsync(AudioMessage audioMessage)
    {
        _context.AudioMessages.Add(audioMessage);
        await _context.SaveChangesAsync();
        return audioMessage;
    }

    public async Task<AudioMessage> UpdateAsync(AudioMessage audioMessage)
    {
        _context.AudioMessages.Update(audioMessage);
        await _context.SaveChangesAsync();
        return audioMessage;
    }

    public async Task<bool> DeleteAsync(Guid id)
    {
        var audioMessage = await _context.AudioMessages.FindAsync(id);
        if (audioMessage == null)
            return false;

        _context.AudioMessages.Remove(audioMessage);
        await _context.SaveChangesAsync();
        return true;
    }

    public async Task<bool> ExistsByMessageIdAsync(string messageId)
    {
        return await _context.AudioMessages
            .AnyAsync(a => a.MessageId == messageId);
    }

    public async Task<long> CountAsync()
    {
        return await _context.AudioMessages.CountAsync();
    }

    public async Task<long> CountByStatusAsync(ProcessingStatus status)
    {
        return await _context.AudioMessages
            .Where(a => a.Status == status)
            .CountAsync();
    }
}