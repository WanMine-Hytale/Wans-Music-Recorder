"""
Generate 12 color variants of Secondary button textures for MIDI octave.
Each note gets a distinct color for easy visual identification.
"""

from PIL import Image, ImageEnhance
import os

# MIDI octave notes with color assignments
# White keys (natural notes): lighter/warmer colors
# Black keys (sharps/flats): darker/cooler colors
MIDI_NOTES = [
    ("C", (255, 100, 100)),      # Red
    ("Cs", (180, 80, 200)),      # Purple (C#/Db - diesis)
    ("D", (255, 165, 100)),      # Orange
    ("Ds", (140, 100, 180)),     # Violet (D#/Eb - diesis)
    ("E", (255, 220, 100)),      # Yellow
    ("F", (100, 255, 150)),      # Light Green
    ("Fs", (80, 160, 200)),      # Cyan (F#/Gb - diesis)
    ("G", (100, 200, 255)),      # Light Blue
    ("Gs", (100, 140, 200)),     # Deep Blue (G#/Ab - diesis)
    ("A", (200, 255, 150)),      # Lime
    ("As", (160, 100, 180)),     # Indigo (A#/Bb - diesis)
    ("B", (255, 150, 200)),      # Pink
]

def apply_color_overlay(image, color, intensity=0.4):
    """Apply a color overlay to an image while preserving transparency and opacity."""
    import numpy as np
    
    # Convert to RGBA if needed
    if image.mode != 'RGBA':
        image = image.convert('RGBA')
    
    # Convert to numpy array for easier manipulation
    img_array = np.array(image, dtype=np.float32)
    
    # Separate RGB and Alpha channels
    rgb = img_array[:, :, :3]
    alpha = img_array[:, :, 3:4]
    
    # Create color overlay (same dimensions as image)
    color_overlay = np.array([color[0], color[1], color[2]], dtype=np.float32)
    
    # Blend RGB channels with color, preserving transparency
    # Only blend where alpha > 0
    rgb_blended = rgb * (1 - intensity) + color_overlay * intensity
    
    # Combine blended RGB with original alpha
    result_array = np.concatenate([rgb_blended, alpha], axis=2)
    result_array = np.clip(result_array, 0, 255).astype(np.uint8)
    
    # Convert back to PIL Image
    result = Image.fromarray(result_array, 'RGBA')
    
    return result

def generate_variants(base_dir):
    """Generate 12 color variants for each Secondary button file."""
    
    # Find ONLY original Secondary button files (exclude note variants)
    all_files = [f for f in os.listdir(base_dir) if f.startswith('Secondary') and f.endswith('@2x.png')]
    
    # Filter to exclude files with note names (already generated variants)
    # Note names patterns: Secondary_C@2x, Secondary_Cs_Middle@2x, etc.
    note_patterns = ['_C@2x', '_Cs@2x', '_D@2x', '_Ds@2x', '_E@2x', '_F@2x', 
                     '_Fs@2x', '_G@2x', '_Gs@2x', '_A@2x', '_As@2x', '_B@2x',
                     '_C_', '_Cs_', '_D_', '_Ds_', '_E_', '_F_', 
                     '_Fs_', '_G_', '_Gs_', '_A_', '_As_', '_B_']
    
    secondary_files = [
        f for f in all_files 
        if not any(pattern in f for pattern in note_patterns)
    ]
    
    print(f"Found {len(secondary_files)} Secondary button files")
    
    for filename in secondary_files:
        base_path = os.path.join(base_dir, filename)
        
        try:
            # Open the base image
            base_image = Image.open(base_path)
            
            # Extract the base name and extension
            name_parts = filename.replace('@2x.png', '').split('_')
            
            # Generate 12 variants
            for note_name, color in MIDI_NOTES:
                # Create new filename with @2x suffix
                if name_parts[0] == 'Secondary' and len(name_parts) == 1:
                    new_filename = f"Secondary_{note_name}@2x.png"
                else:
                    # Handle Secondary_Hovered, Secondary_Pressed, etc.
                    suffix = '_'.join(name_parts[1:])
                    new_filename = f"Secondary_{note_name}_{suffix}@2x.png"
                
                new_path = os.path.join(base_dir, new_filename)
                
                # Apply color overlay
                colored_image = apply_color_overlay(base_image.copy(), color, intensity=0.4)
                
                # Save the variant
                colored_image.save(new_path)
                print(f"Created: {new_filename}")
                
        except Exception as e:
            print(f"Error processing {filename}: {e}")

if __name__ == "__main__":
    base_dir = r"d:\WansMusicRecorder\src\main\resources\Common\UI\Custom\ButtonTextures"
    
    print("Generating MIDI octave button variants...")
    print("=" * 60)
    generate_variants(base_dir)
    print("=" * 60)
    print("Done! Created 12 variants for each Secondary button.")
    print("\nNote mapping:")
    for i, (note, color) in enumerate(MIDI_NOTES):
        note_type = "(♯/♭)" if note.endswith('s') else ""
        print(f"{i+1:2d}. {note:3s} {note_type:6s} - RGB{color}")
