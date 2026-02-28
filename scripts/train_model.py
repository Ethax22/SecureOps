#!/usr/bin/env python3
"""
TensorFlow Lite Model Training Script for SecureOps CI/CD Failure Prediction

Trains a binary classification model on pipeline failure data and exports to TFLite format.
"""

import os
import sys
import numpy as np
import pandas as pd
import tensorflow as tf
from sklearn.model_selection import train_test_split
from sklearn.metrics import accuracy_score, precision_score, recall_score, f1_score, confusion_matrix
from datetime import datetime

# Configuration
INPUT_CSV = 'training_dataset.csv'
OUTPUT_MODEL = '../app/src/main/assets/failure_prediction_v2.tflite'
TEST_SIZE = 0.2
RANDOM_STATE = 42
EPOCHS = 50
BATCH_SIZE = 32
VALIDATION_SPLIT = 0.2

def print_header(message):
    """Print formatted header"""
    print("\n" + "="*70)
    print(f"  {message}")
    print("="*70)

def load_dataset(csv_path):
    """Load and prepare dataset from CSV"""
    print_header("Loading Dataset")
    
    if not os.path.exists(csv_path):
        print(f"❌ Error: Dataset not found at {csv_path}")
        print(f"   Please run the Android app to generate training data first.")
        sys.exit(1)
    
    # Load CSV
    df = pd.read_csv(csv_path)
    print(f"✅ Loaded dataset: {csv_path}")
    print(f"   Total samples: {len(df)}")
    print(f"   Features: {len(df.columns) - 1}")
    
    # Separate features and labels
    X = df.drop('label', axis=1).values.astype(np.float32)
    y = df['label'].values.astype(np.float32)
    
    # Print class distribution
    unique, counts = np.unique(y, return_counts=True)
    print(f"\n📊 Class Distribution:")
    print(f"   Success (0): {counts[0]} samples ({counts[0]/len(y)*100:.1f}%)")
    print(f"   Failure (1): {counts[1]} samples ({counts[1]/len(y)*100:.1f}%)")
    
    return X, y, df.columns[:-1].tolist()

def create_model(input_shape):
    """Create neural network model"""
    print_header("Building Model")
    
    model = tf.keras.Sequential([
        tf.keras.layers.Input(shape=(input_shape,)),
        tf.keras.layers.Dense(64, activation='relu', name='dense_1'),
        tf.keras.layers.Dropout(0.3, name='dropout_1'),
        tf.keras.layers.Dense(32, activation='relu', name='dense_2'),
        tf.keras.layers.Dropout(0.3, name='dropout_2'),
        tf.keras.layers.Dense(1, activation='sigmoid', name='output')
    ])
    
    # Compile model
    model.compile(
        optimizer=tf.keras.optimizers.Adam(learning_rate=0.001),
        loss='binary_crossentropy',
        metrics=[
            'accuracy',
            tf.keras.metrics.Precision(name='precision'),
            tf.keras.metrics.Recall(name='recall')
        ]
    )
    
    print("✅ Model architecture:")
    model.summary()
    
    return model

def train_model(model, X_train, y_train):
    """Train the model"""
    print_header("Training Model")
    
    # Callbacks
    callbacks = [
        tf.keras.callbacks.EarlyStopping(
            monitor='val_loss',
            patience=10,
            restore_best_weights=True,
            verbose=1
        ),
        tf.keras.callbacks.ReduceLROnPlateau(
            monitor='val_loss',
            factor=0.5,
            patience=5,
            min_lr=0.00001,
            verbose=1
        )
    ]
    
    # Train
    print(f"🚀 Training for {EPOCHS} epochs...")
    print(f"   Batch size: {BATCH_SIZE}")
    print(f"   Validation split: {VALIDATION_SPLIT}")
    
    history = model.fit(
        X_train, y_train,
        epochs=EPOCHS,
        batch_size=BATCH_SIZE,
        validation_split=VALIDATION_SPLIT,
        callbacks=callbacks,
        verbose=1
    )
    
    print("\n✅ Training complete!")
    
    return history

def evaluate_model(model, X_test, y_test):
    """Evaluate model on test set"""
    print_header("Model Evaluation")
    
    # Get predictions
    y_pred_proba = model.predict(X_test, verbose=0)
    y_pred = (y_pred_proba > 0.5).astype(int).flatten()
    
    # Calculate metrics
    accuracy = accuracy_score(y_test, y_pred)
    precision = precision_score(y_test, y_pred)
    recall = recall_score(y_test, y_pred)
    f1 = f1_score(y_test, y_pred)
    
    # Confusion matrix
    cm = confusion_matrix(y_test, y_pred)
    tn, fp, fn, tp = cm.ravel()
    
    # Print results
    print(f"📊 Test Set Performance:")
    print(f"   Accuracy:  {accuracy:.4f} ({accuracy*100:.2f}%)")
    print(f"   Precision: {precision:.4f} ({precision*100:.2f}%)")
    print(f"   Recall:    {recall:.4f} ({recall*100:.2f}%)")
    print(f"   F1 Score:  {f1:.4f} ({f1*100:.2f}%)")
    
    print(f"\n📈 Confusion Matrix:")
    print(f"   True Negatives:  {tn}")
    print(f"   False Positives: {fp}")
    print(f"   False Negatives: {fn}")
    print(f"   True Positives:  {tp}")
    
    # Calculate additional metrics
    specificity = tn / (tn + fp) if (tn + fp) > 0 else 0
    print(f"\n🎯 Additional Metrics:")
    print(f"   Specificity: {specificity:.4f}")
    print(f"   False Positive Rate: {fp/(fp+tn):.4f}" if (fp+tn) > 0 else "   False Positive Rate: N/A")
    print(f"   False Negative Rate: {fn/(fn+tp):.4f}" if (fn+tp) > 0 else "   False Negative Rate: N/A")
    
    # Check if model meets production thresholds
    print(f"\n✅ Production Thresholds:")
    print(f"   Precision ≥ 0.85: {'✅ PASS' if precision >= 0.85 else '❌ FAIL'}")
    print(f"   Recall ≥ 0.80:    {'✅ PASS' if recall >= 0.80 else '❌ FAIL'}")
    print(f"   F1 ≥ 0.82:        {'✅ PASS' if f1 >= 0.82 else '❌ FAIL'}")
    
    return {
        'accuracy': accuracy,
        'precision': precision,
        'recall': recall,
        'f1': f1,
        'confusion_matrix': cm
    }

def convert_to_tflite(model, output_path):
    """Convert Keras model to quantized TFLite format"""
    print_header("Converting to TFLite")
    
    # Create converter
    converter = tf.lite.TFLiteConverter.from_keras_model(model)
    
    # Apply optimizations (quantization)
    converter.optimizations = [tf.lite.Optimize.DEFAULT]
    
    # Convert
    print("🔄 Converting model to TFLite format...")
    tflite_model = converter.convert()
    
    # Ensure output directory exists
    output_dir = os.path.dirname(output_path)
    if output_dir and not os.path.exists(output_dir):
        os.makedirs(output_dir)
    
    # Save to file
    with open(output_path, 'wb') as f:
        f.write(tflite_model)
    
    # Get file size
    size_bytes = len(tflite_model)
    size_kb = size_bytes / 1024
    size_mb = size_kb / 1024
    
    print(f"✅ TFLite model saved: {output_path}")
    print(f"   Model size: {size_mb:.2f} MB ({size_kb:.2f} KB)")
    
    # Verify model
    print("\n🔍 Verifying TFLite model...")
    interpreter = tf.lite.Interpreter(model_path=output_path)
    interpreter.allocate_tensors()
    
    input_details = interpreter.get_input_details()
    output_details = interpreter.get_output_details()
    
    print(f"   Input shape: {input_details[0]['shape']}")
    print(f"   Output shape: {output_details[0]['shape']}")
    print(f"   Input type: {input_details[0]['dtype']}")
    print(f"   Output type: {output_details[0]['dtype']}")
    
    return output_path

def save_training_report(metrics, feature_names, output_dir='.'):
    """Save training report to file"""
    timestamp = datetime.now().strftime("%Y%m%d_%H%M%S")
    report_path = os.path.join(output_dir, f'training_report_{timestamp}.txt')
    
    with open(report_path, 'w', encoding='utf-8') as f:
        f.write("="*70 + "\n")
        f.write("SecureOps ML Model Training Report\n")
        f.write("="*70 + "\n\n")
        f.write(f"Date: {datetime.now().strftime('%Y-%m-%d %H:%M:%S')}\n")
        f.write(f"TensorFlow Version: {tf.__version__}\n\n")
        
        f.write("Model Architecture:\n")
        f.write("  Dense(64, relu) → Dropout(0.3) → Dense(32, relu) → Dropout(0.3) → Dense(1, sigmoid)\n\n")
        
        f.write("Training Configuration:\n")
        f.write(f"  Epochs: {EPOCHS}\n")
        f.write(f"  Batch Size: {BATCH_SIZE}\n")
        f.write(f"  Validation Split: {VALIDATION_SPLIT}\n")
        f.write(f"  Test Split: {TEST_SIZE}\n\n")
        
        f.write("Features (13):\n")
        for i, feature in enumerate(feature_names, 1):
            f.write(f"  {i}. {feature}\n")
        f.write("\n")
        
        f.write("Test Set Performance:\n")
        f.write(f"  Accuracy:  {metrics['accuracy']:.4f}\n")
        f.write(f"  Precision: {metrics['precision']:.4f}\n")
        f.write(f"  Recall:    {metrics['recall']:.4f}\n")
        f.write(f"  F1 Score:  {metrics['f1']:.4f}\n\n")
        
        f.write("Confusion Matrix:\n")
        cm = metrics['confusion_matrix']
        f.write(f"  [[{cm[0,0]}, {cm[0,1]}],\n")
        f.write(f"   [{cm[1,0]}, {cm[1,1]}]]\n\n")
        
        f.write("="*70 + "\n")
    
    print(f"\n📄 Training report saved: {report_path}")

def main():
    """Main training pipeline"""
    print_header("SecureOps ML Model Training")
    print(f"TensorFlow Version: {tf.__version__}")
    print(f"Timestamp: {datetime.now().strftime('%Y-%m-%d %H:%M:%S')}")
    
    # Check for GPU
    gpus = tf.config.list_physical_devices('GPU')
    if gpus:
        print(f"🎮 GPU Available: {len(gpus)} device(s)")
    else:
        print("💻 Running on CPU")
    
    # 1. Load dataset
    X, y, feature_names = load_dataset(INPUT_CSV)
    
    # 2. Split data
    print_header("Splitting Dataset")
    X_train, X_test, y_train, y_test = train_test_split(
        X, y, 
        test_size=TEST_SIZE, 
        random_state=RANDOM_STATE,
        stratify=y
    )
    print(f"✅ Data split (80/20):")
    print(f"   Training samples: {len(X_train)}")
    print(f"   Test samples:     {len(X_test)}")
    
    # 3. Create model
    model = create_model(input_shape=X.shape[1])
    
    # 4. Train model
    history = train_model(model, X_train, y_train)
    
    # 5. Evaluate model
    metrics = evaluate_model(model, X_test, y_test)
    
    # 6. Convert to TFLite
    tflite_path = convert_to_tflite(model, OUTPUT_MODEL)
    
    # 7. Save report
    save_training_report(metrics, feature_names)
    
    # Final summary
    print_header("Training Complete")
    print(f"✅ Model saved to: {tflite_path}")
    print(f"✅ Accuracy:  {metrics['accuracy']*100:.2f}%")
    print(f"✅ Precision: {metrics['precision']*100:.2f}%")
    print(f"✅ Recall:    {metrics['recall']*100:.2f}%")
    print(f"✅ F1 Score:  {metrics['f1']*100:.2f}%")
    
    # Check if production ready
    is_production_ready = (
        metrics['precision'] >= 0.85 and 
        metrics['recall'] >= 0.80 and 
        metrics['f1'] >= 0.82
    )
    
    if is_production_ready:
        print("\n🎉 Model meets production thresholds! Ready for deployment.")
    else:
        print("\n⚠️  Model does not meet all production thresholds.")
        print("    Consider collecting more data or tuning hyperparameters.")
    
    print("\n" + "="*70 + "\n")

if __name__ == '__main__':
    try:
        main()
    except KeyboardInterrupt:
        print("\n\n⚠️  Training interrupted by user")
        sys.exit(1)
    except Exception as e:
        print(f"\n\n❌ Error: {str(e)}")
        import traceback
        traceback.print_exc()
        sys.exit(1)
