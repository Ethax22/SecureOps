# ML Model Training Scripts

## Overview

This directory contains Python scripts for training the TensorFlow Lite model used by SecureOps for CI/CD pipeline failure prediction.

## Prerequisites

- Python 3.8 or higher
- pip package manager

## Setup

1. Install required dependencies:

```bash
pip install -r requirements.txt
```

Or using a virtual environment (recommended):

```bash
python -m venv venv
source venv/bin/activate  # On Windows: venv\Scripts\activate
pip install -r requirements.txt
```

## Usage

### 1. Generate Training Data

First, run the SecureOps Android app to generate the training dataset:

1. Open the app
2. Ensure you have synced pipelines from your CI/CD providers
3. Use the `TrainingDatasetManager` to export data:

```kotlin
// In your Android code
val trainingManager: TrainingDatasetManager by inject()

lifecycleScope.launch {
    val result = trainingManager.generateAndExportDataset(
        minSamples = 100,
        balanceRatio = 0.5f,
        exportFormat = ExportFormat.CSV
    )
    
    when (result) {
        is TrainingResult.Success -> {
            println("Dataset exported: ${result.filePath}")
            // Copy this file to scripts/ directory
        }
    }
}
```

2. Copy the exported `training_dataset.csv` to the `scripts/` directory.

### 2. Train the Model

Run the training script:

```bash
cd scripts
python train_model.py
```

The script will:
- ✅ Load the CSV dataset
- ✅ Split data 80/20 (train/test)
- ✅ Build neural network architecture
- ✅ Train for 50 epochs with early stopping
- ✅ Evaluate on test set
- ✅ Print metrics (accuracy, precision, recall, F1)
- ✅ Convert to quantized TFLite format
- ✅ Save model to `../app/src/main/assets/failure_prediction_v2.tflite`
- ✅ Generate training report

### 3. Deploy the Model

The trained model is automatically saved to the Android app's assets directory:

```
app/src/main/assets/failure_prediction_v2.tflite
```

Rebuild the Android app to include the new model.

## Model Architecture

```
Input (13 features)
    ↓
Dense(64, relu)
    ↓
Dropout(0.3)
    ↓
Dense(32, relu)
    ↓
Dropout(0.3)
    ↓
Dense(1, sigmoid)
    ↓
Output (failure probability 0-1)
```

## Features (13)

1. `commit_size` - Normalized commit size (0-1)
2. `test_failure_rate` - Historical test failure rate
3. `code_complexity` - Estimated code complexity
4. `test_coverage_change` - Change in test coverage
5. `error_pattern_count` - Number of error patterns in logs
6. `warning_count` - Number of warnings in logs
7. `build_stability` - Recent build success rate
8. `commit_sentiment` - Sentiment analysis of commit message
9. `dependency_changes` - Whether dependencies were changed
10. `config_changes` - Whether config files were changed
11. `hour_of_day` - Time of day build was triggered
12. `day_of_week` - Day of week build was triggered
13. `author_failure_rate` - Author's historical failure rate

## Production Thresholds

The model must meet these thresholds for production deployment:

- **Precision ≥ 0.85** (85%)
- **Recall ≥ 0.80** (80%)
- **F1 Score ≥ 0.82** (82%)

## Output Files

- `failure_prediction_v2.tflite` - Quantized TFLite model (~50-100 KB)
- `training_report_YYYYMMDD_HHMMSS.txt` - Detailed training report

## Troubleshooting

### "Dataset not found" Error

Make sure you've generated the training data from the Android app and copied `training_dataset.csv` to the `scripts/` directory.

### Low Model Accuracy

If the model doesn't meet production thresholds:

1. **Collect more data** - Ensure you have at least 500 samples
2. **Balance the dataset** - Ensure roughly equal success/failure samples
3. **Increase epochs** - Edit `EPOCHS` in `train_model.py`
4. **Tune hyperparameters** - Adjust learning rate, batch size, or architecture

### GPU Support

To use GPU acceleration:

```bash
pip install tensorflow-gpu
```

Ensure you have CUDA and cuDNN installed.

## Advanced Usage

### Custom Configuration

Edit the configuration section in `train_model.py`:

```python
TEST_SIZE = 0.2          # Test set ratio
RANDOM_STATE = 42        # Random seed for reproducibility
EPOCHS = 50              # Maximum training epochs
BATCH_SIZE = 32          # Training batch size
VALIDATION_SPLIT = 0.2   # Validation split from training data
```

### Repository-Specific Models

To train a model for a specific repository:

```kotlin
val result = trainingManager.generateDatasetForRepository(
    repositoryName = "my-app",
    minSamples = 50
)
```

## License

Part of the SecureOps project.
