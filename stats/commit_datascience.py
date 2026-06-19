import pandas as pd
import matplotlib.pyplot as plt
from pathlib import Path
import sys

# ============================================================================
# CONFIGURATION
# ============================================================================

# Map variant author names to canonical names
AUTHOR_ALIAS = {
    "ChrisHorler": "Christian",
    "Christian": "Christian",
    "Liam Willis": "Liam",
    "Martijn": "Martijn",
    "oldmartijntje": "Martijn",
    "Angel": "Angel"
    # Add more mappings as needed
}
IGNORED_SHAS = [
    "5524a234a2eeefc89debc9adb9c6f84a6a680f9e",
    "67bb25bf76c51b514b872f28b13939ffda00f413",
    "cf37e3cecf657e62a3b927ea5653c0d53bcae90a",
    "2d4920e77989a6d44973ad321fc335f1d11e68cf",
    "0f619c026e5cc70423e656b67b6eab4aceb87fa5",
    "9038ce4d13802672cec411ec0a405be8c04e189b",
    "d799db64d38d3ce1daa9f7d0cdc64023b7d36e2e"
]

# Commit messages to ignore (substring match)
IGNORED_MESSAGE_PATTERNS = [
    "Copilot@users.noreply.github.com",
]

# List of CSV files to process
COMMIT_FILES = [
    "data/audit_commits.csv",
    "data/docs_commits.csv",
]

OUTPUT_DIR = Path("output/output-total")
OUTPUT_CSV = OUTPUT_DIR / "combined_commits.csv"

# ============================================================================
# FUNCTIONS
# ============================================================================


def load_and_merge_commits(files):
    """Load multiple CSV files and merge into one DataFrame."""
    dfs = []
    for file in files:
        if not Path(file).exists():
            print(f"Warning: {file} not found, skipping...")
            continue
        try:
            df = pd.read_csv(file)
            dfs.append(df)
            print(f"Loaded {file}: {len(df)} commits")
        except Exception as e:
            print(f"Error loading {file}: {e}")
            continue

    if not dfs:
        print("No files loaded!")
        sys.exit(1)

    combined = pd.concat(dfs, ignore_index=True)
    print(f"\nTotal combined commits: {len(combined)}")
    return combined


def normalize_authors(df, alias_map):
    """Normalize author names using alias dictionary."""
    df["author_name"] = df["author_name"].map(alias_map).fillna(
        df["author_name"]
    )
    return df

def plot_average_metrics(series_by_author, output_dir):
    """Plot average additions, deletions, changes across all authors."""
    output_dir.mkdir(exist_ok=True)
    
    # Sum metrics across all authors for each date
    metrics_over_time = {}
    all_dates = series_by_author[next(iter(series_by_author))].index
    
    for metric in ["insertions", "deletions", "total_changes"]:
        daily_totals = []
        for date in all_dates:
            total = sum(
                series_by_author[author].loc[date, metric]
                for author in series_by_author
                if date in series_by_author[author].index
            )
            daily_totals.append(total)
        metrics_over_time[metric] = daily_totals
    
    # Create plot
    plt.figure(figsize=(14, 6))
    
    plt.plot(all_dates, metrics_over_time["insertions"], 
             label="Insertions", color="#2ca02c", linewidth=2, marker="o",
             markersize=3)
    plt.plot(all_dates, metrics_over_time["deletions"], 
             label="Deletions", color="#d62728", linewidth=2, marker="o",
             markersize=3)
    plt.plot(all_dates, metrics_over_time["total_changes"], 
             label="Total Changes", color="#1f77b4", linewidth=2, marker="o",
             markersize=3)
    
    plt.title("Team Average Code Activity Over Time", fontsize=14,
              fontweight="bold")
    plt.xlabel("Date", fontsize=11)
    plt.ylabel("Changes", fontsize=11)
    plt.legend(loc="best", frameon=True)
    plt.grid(True, alpha=0.3)
    plt.xticks(rotation=45)
    plt.tight_layout()
    
    output_file = output_dir / "team_average_metrics.png"
    plt.savefig(output_file, dpi=150)
    print(f"Saved: {output_file}")
    plt.close()


def plot_commits_by_hour(df, output_dir):
    """Create heatmap and individual plots for commits by hour per author."""
    output_dir.mkdir(exist_ok=True)
    
    # Extract hour from datetime, converting to UTC first
    df = df.copy()
    df["date"] = pd.to_datetime(df["date"], utc=True)
    df["hour"] = df["date"].dt.hour
    
    # Aggregate by author and hour
    hourly_commits = (
        df.groupby(["author_name", "hour"], as_index=False)
        .agg(commit_count=("hexsha", "count"))
    )
    
    # Create pivot table for heatmap
    pivot_data = hourly_commits.pivot(
        index="author_name",
        columns="hour",
        values="commit_count"
    ).fillna(0)
    
    # Ensure all hours 0-23 are present
    for hour in range(24):
        if hour not in pivot_data.columns:
            pivot_data[hour] = 0
    pivot_data = pivot_data[[col for col in range(24)]]
    
    # Heatmap
    import numpy as np
    fig, ax = plt.subplots(figsize=(14, max(4, len(pivot_data) * 0.4)))
    im = ax.imshow(pivot_data.values, cmap="YlOrRd", aspect="auto")
    
    ax.set_xticks(range(24))
    ax.set_xticklabels(range(24))
    ax.set_yticks(range(len(pivot_data)))
    ax.set_yticklabels(pivot_data.index)
    ax.set_xlabel("Hour of Day (UTC)", fontsize=11)
    ax.set_ylabel("Author", fontsize=11)
    ax.set_title("Commit Activity by Hour of Day (Per Author)", fontsize=14,
                 fontweight="bold")
    
    plt.colorbar(im, ax=ax, label="Number of Commits")
    plt.tight_layout()
    
    output_file = output_dir / "commits_by_hour_heatmap.png"
    plt.savefig(output_file, dpi=150)
    print(f"Saved: {output_file}")
    plt.close()
    
    # Individual bar charts for each author
    for author in sorted(pivot_data.index):
        plt.figure(figsize=(12, 5))
        hours = pivot_data.columns
        commits = pivot_data.loc[author].values
        
        plt.bar(hours, commits, color="#1f77b4", alpha=0.7, edgecolor="black")
        plt.title(f"{author} - Commits by Hour of Day (UTC)", fontsize=12,
                  fontweight="bold")
        plt.xlabel("Hour of Day (UTC)", fontsize=11)
        plt.ylabel("Number of Commits", fontsize=11)
        plt.xticks(range(24))
        plt.grid(True, alpha=0.3, axis="y")
        plt.tight_layout()
        
        safe_name = author.replace(" ", "_").replace("/", "_")
        output_file = output_dir / f"commits_by_hour_{safe_name}.png"
        plt.savefig(output_file, dpi=150)
        print(f"Saved: {output_file}")
        plt.close()

def detect_outliers(df, output_dir):
    """Detect and print commits with unusually high changes."""
    output_dir.mkdir(exist_ok=True)
    
    # Calculate statistics
    mean_changes = df["total_changes"].mean()
    std_changes = df["total_changes"].std()
    threshold = mean_changes + (2 * std_changes)  # 2 sigma
    
    # Find outliers
    outliers = df[df["total_changes"] > threshold].sort_values(
        "total_changes", ascending=False
    )
    
    print(f"\n{'='*70}")
    print(f"OUTLIER DETECTION (threshold: {threshold:.0f} changes)")
    print(f"{'='*70}")
    print(f"Found {len(outliers)} outlier commits\n")
    
    for idx, row in outliers.iterrows():
        print(f"SHA: {row['hexsha']}")
        print(f"  Author: {row['author_name']}")
        print(f"  Date: {row['date']}")
        print(f"  Changes: {row['total_changes']} "
              f"(+{row['insertions']}/-{row['deletions']})")
        print(f"  Message: {row['message'][:60]}...")
        print()
    
    # Also save to CSV
    if not outliers.empty:
        outliers_csv = output_dir / "outliers.csv"
        outliers.to_csv(outliers_csv, index=False)
        print(f"Outliers saved to: {outliers_csv}\n")


def clean_data(df):
    """Remove merge commits, ignored SHAs, patterns, and convert date."""
    # Make a copy to avoid SettingWithCopyWarning
    df = df.copy()

    # Filter out commits with ignored message patterns
    if "message" in df.columns:
        before = len(df)
        mask = df["message"].astype(str).str.contains(
            "|".join(IGNORED_MESSAGE_PATTERNS),
            case=False,
            na=False,
            regex=False
        )
        df = df[~mask].copy()
        filtered = before - len(df)
        if filtered > 0:
            print(f"Filtered ignored message patterns: {filtered} commits removed")
    else:
        print("Warning: 'message' column not found (can't filter by message)")

    # Filter out ignored commits by SHA
    if "hexsha" in df.columns:
        before = len(df)
        df = df[~df["hexsha"].isin(IGNORED_SHAS)].copy()
        filtered = before - len(df)
        if filtered > 0:
            print(f"Filtered ignored SHAs: {filtered} commits removed")
    else:
        print("Warning: 'sha' column not found (can't filter by SHA)")

    # Filter out merge commits if column exists
    if "is_merge" in df.columns:
        before = len(df)
        df = df[df["is_merge"] != True].copy()
        print(f"Filtered merge commits: {before} → {len(df)}")
    else:
        print("Warning: 'is_merge' column not found")

    # Convert date to datetime, handling mixed timezones
    df["date"] = pd.to_datetime(
        df["date"],
        utc=True,
        errors="coerce"
    )

    # Remove rows where date conversion failed
    before = len(df)
    df = df.dropna(subset=["date"])
    if len(df) < before:
        print(
            f"Warning: {before - len(df)} rows had invalid dates "
            "and were removed"
        )

    # Convert to timezone-naive
    df["date"] = df["date"].dt.tz_localize(None)

    return df


def aggregate_by_day(df):
    """Aggregate insertions and deletions per author per day."""
    df = df.copy()
    df["total_changes"] = df["insertions"] + df["deletions"]

    # Ensure date is datetime and extract date properly
    df["date_only"] = df["date"].dt.date

    agg = (
        df.groupby(
            ["author_name", "date_only"],
            as_index=False
        )
        .agg(
            insertions=("insertions", "sum"),
            deletions=("deletions", "sum"),
            total_changes=("total_changes", "sum"),
            commit_count=("author_name", "count"),
        )
        .rename(columns={"date_only": "date"})
    )

    agg["date"] = pd.to_datetime(agg["date"])
    return agg


def align_to_date_range(agg_df):
    """
    Align all authors to the same date range with zero-filling.
    Returns dict: {author_name: reindexed_dataframe}
    """
    min_date = agg_df["date"].min()
    max_date = agg_df["date"].max()
    all_dates = pd.date_range(min_date, max_date)

    print(
        f"\nAligning to date range: {min_date.date()} → "
        f"{max_date.date()} ({len(all_dates)} days)"
    )

    series_by_author = {}
    for author in agg_df["author_name"].unique():
        author_df = agg_df[agg_df["author_name"] == author].copy()
        author_df = author_df.set_index("date")
        # Drop author_name column before reindexing to avoid type issues
        author_df = author_df.drop(columns=["author_name"])
        author_df = author_df.reindex(all_dates, fill_value=0)
        author_df = author_df.rename_axis("date")
        series_by_author[author] = author_df

    return series_by_author


def plot_metrics(series_by_author, output_dir):
    """Create and save time-series plots for each metric."""
    output_dir.mkdir(exist_ok=True)
    metrics = ["total_changes", "insertions", "deletions"]

    for metric in metrics:
        plt.figure(figsize=(14, 6))

        for author, data in series_by_author.items():
            plt.plot(
                data.index,
                data[metric],
                label=author,
                marker="o",
                markersize=3,
                linewidth=1.5,
                alpha=0.8,
            )

        title = metric.replace("_", " ").title()
        plt.title(f"{title} Over Time (Per Author)", fontsize=14, fontweight="bold")
        plt.xlabel("Date", fontsize=11)
        plt.ylabel(title, fontsize=11)
        plt.legend(loc="best", frameon=True)
        plt.grid(True, alpha=0.3)
        plt.xticks(rotation=45)
        plt.tight_layout()

        output_file = output_dir / f"{metric}_by_author.png"
        plt.savefig(output_file, dpi=150)
        print(f"Saved: {output_file}")
        plt.close()


def plot_individual_authors(series_by_author, output_dir):
    """Create individual subplots for each author."""
    output_dir.mkdir(exist_ok=True)
    authors = sorted(series_by_author.keys())

    for author in authors:
        data = series_by_author[author]

        fig, axes = plt.subplots(3, 1, figsize=(12, 8), sharex=True)

        metrics = ["total_changes", "insertions", "deletions"]
        colors = ["#1f77b4", "#2ca02c", "#d62728"]

        for ax, metric, color in zip(axes, metrics, colors):
            ax.plot(data.index, data[metric], color=color, linewidth=2)
            ax.set_ylabel(metric.replace("_", " ").title())
            ax.grid(True, alpha=0.3)
            ax.fill_between(data.index, data[metric], alpha=0.3, color=color)

        axes[0].set_title(f"{author} - Code Activity", fontsize=12,
                         fontweight="bold")
        axes[-1].set_xlabel("Date")
        plt.xticks(rotation=45)
        plt.tight_layout()

        safe_name = author.replace(" ", "_").replace("/", "_")
        output_file = output_dir / f"author_{safe_name}.png"
        plt.savefig(output_file, dpi=150)
        print(f"Saved: {output_file}")
        plt.close()


def save_combined_csv(df, output_file):
    """Save combined and normalized dataset to CSV."""
    output_file.parent.mkdir(exist_ok=True)
    df = df.sort_values("date")
    df.to_csv(output_file, index=False)
    print(f"Saved combined data: {output_file}")

def plot_commit_counts(series_by_author, output_dir):
    """Create and save commit count plots."""
    output_dir.mkdir(exist_ok=True)

    # Combined plot: all authors
    plt.figure(figsize=(14, 6))
    for author, data in series_by_author.items():
        plt.plot(
            data.index,
            data["commit_count"],
            label=author,
            marker="o",
            markersize=3,
            linewidth=1.5,
            alpha=0.8,
        )

    plt.title("Commit Count Over Time (Per Author)", fontsize=14,
              fontweight="bold")
    plt.xlabel("Date", fontsize=11)
    plt.ylabel("Number of Commits", fontsize=11)
    plt.legend(loc="best", frameon=True)
    plt.grid(True, alpha=0.3)
    plt.xticks(rotation=45)
    plt.tight_layout()

    output_file = output_dir / "commit_count_by_author.png"
    plt.savefig(output_file, dpi=150)
    print(f"Saved: {output_file}")
    plt.close()


# ============================================================================
# MAIN
# ============================================================================


def main():
    print("=" * 70)
    print("Multi-Repo Commit Analysis")
    print("=" * 70)

    # 1. Load and merge
    df = load_and_merge_commits(COMMIT_FILES)

    # 2. Normalize authors
    df = normalize_authors(df, AUTHOR_ALIAS)
    print(f"Unique authors after normalization: {df['author_name'].nunique()}")

    # 3. Clean data
    df = clean_data(df)

    detect_outliers(df, OUTPUT_DIR)

    # 4. Aggregate by day
    agg = aggregate_by_day(df)
    print(f"Unique authors in aggregated data: {agg['author_name'].nunique()}")

    # 5. Align to date range
    series_by_author = align_to_date_range(agg)

    # 6. Save combined CSV
    save_combined_csv(agg, OUTPUT_CSV)

    # 7. Generate plots
    print("\nGenerating plots...")
    plot_metrics(series_by_author, OUTPUT_DIR)
    plot_commit_counts(series_by_author, OUTPUT_DIR)
    plot_individual_authors(series_by_author, OUTPUT_DIR)
    plot_average_metrics(series_by_author, OUTPUT_DIR)
    plot_commits_by_hour(df, OUTPUT_DIR)

    print("\n" + "=" * 70)
    print("Analysis complete!")
    print("=" * 70)


if __name__ == "__main__":
    main()