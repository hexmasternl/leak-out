using Microsoft.EntityFrameworkCore;

namespace HexMaster.LeakOut.Players.Data.PostgreSQL;

public sealed class PlayersDbContext(DbContextOptions<PlayersDbContext> options) : DbContext(options)
{
    public DbSet<PlayerEntity> Players => Set<PlayerEntity>();

    protected override void OnModelCreating(ModelBuilder modelBuilder)
    {
        modelBuilder.Entity<PlayerEntity>(player =>
        {
            player.ToTable("players");
            player.HasKey(p => p.Id).HasName("pk_players");
            player.Property(p => p.Id).HasColumnName("id").ValueGeneratedNever();
            player.Property(p => p.SubjectId).HasColumnName("subject_id").IsRequired();
            player.Property(p => p.DisplayName).HasColumnName("display_name").IsRequired();
            player.Property(p => p.CreatedAt).HasColumnName("created_at");
            player.Property(p => p.LastSignedInAt).HasColumnName("last_signed_in_at");
            player.HasIndex(p => p.SubjectId).IsUnique().HasDatabaseName("ix_players_subject_id");
        });
    }
}
