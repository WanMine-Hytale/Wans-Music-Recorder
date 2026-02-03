"""
Generate 12 MIDI note variants of EnabledButton UI files.
Updates texture paths to use the color-coded Secondary button variants.
"""

import os

# MIDI note names (matching the texture file naming)
MIDI_NOTES = ["C", "Cs", "D", "Ds", "E", "F", "Fs", "G", "Gs", "A", "As", "B"]

# Button file configurations
BUTTON_CONFIGS = [
    {
        "source": "EnabledNoteButton.ui",
        "texture_base": "Secondary",
        "texture_hovered": "Secondary_Hovered",
        "texture_pressed": "Secondary_Pressed",
    },
    {
        "source": "EnabledStartNoteButton.ui",
        "texture_base": "Secondary_Start",
        "texture_hovered": "Secondary_Hovered_Start",
        "texture_pressed": "Secondary_Pressed_Start",
    },
    {
        "source": "EnabledMiddleNoteButton.ui",
        "texture_base": "Secondary_Middle",
        "texture_hovered": "Secondary_Hovered_Middle",
        "texture_pressed": "Secondary_Pressed_Middle",
    },
    {
        "source": "EnabledEndNoteButton.ui",
        "texture_base": "Secondary_End",
        "texture_hovered": "Secondary_Hovered_End",
        "texture_pressed": "Secondary_Pressed_End",
    },
]

def generate_button_variant(source_path, note, texture_base, texture_hovered, texture_pressed):
    """Generate a single button variant for a specific MIDI note."""
    
    # Read the original file
    with open(source_path, 'r', encoding='utf-8') as f:
        content = f.read()
    
    # Helper function to insert note name after "Secondary"
    def insert_note_after_secondary(texture_name, note):
        # Split at "Secondary" and insert note right after
        if texture_name.startswith("Secondary"):
            # Replace "Secondary" with "Secondary_[Note]"
            return texture_name.replace("Secondary", f"Secondary_{note}", 1)
        return texture_name
    
    # Replace texture paths with note-specific variants
    new_content = content.replace(
        f'TexturePath: "../../../ButtonTextures/{texture_base}.png"',
        f'TexturePath: "../../../ButtonTextures/{insert_note_after_secondary(texture_base, note)}.png"'
    )
    new_content = new_content.replace(
        f'TexturePath: "../../../ButtonTextures/{texture_hovered}.png"',
        f'TexturePath: "../../../ButtonTextures/{insert_note_after_secondary(texture_hovered, note)}.png"'
    )
    new_content = new_content.replace(
        f'TexturePath: "../../../ButtonTextures/{texture_pressed}.png"',
        f'TexturePath: "../../../ButtonTextures/{insert_note_after_secondary(texture_pressed, note)}.png"'
    )
    
    return new_content

def generate_all_variants(buttons_dir):
    """Generate all button variants for all 12 MIDI notes."""
    
    print("Generating MIDI octave button UI variants...")
    print("=" * 60)
    
    total_created = 0
    
    for config in BUTTON_CONFIGS:
        source_file = config["source"]
        source_path = os.path.join(buttons_dir, source_file)
        
        if not os.path.exists(source_path):
            print(f"Warning: Source file not found: {source_file}")
            continue
        
        # Extract base name (remove 'Enabled' prefix and '.ui' suffix)
        base_name = source_file.replace("Enabled", "").replace(".ui", "")
        
        for note in MIDI_NOTES:
            # Generate new filename
            new_filename = f"Enabled{note}{base_name}.ui"
            new_path = os.path.join(buttons_dir, new_filename)
            
            # Generate content with updated texture paths
            new_content = generate_button_variant(
                source_path,
                note,
                config["texture_base"],
                config["texture_hovered"],
                config["texture_pressed"]
            )
            
            # Write the new file
            with open(new_path, 'w', encoding='utf-8') as f:
                f.write(new_content)
            
            print(f"Created: {new_filename}")
            total_created += 1
    
    print("=" * 60)
    print(f"Done! Created {total_created} button UI files.")
    print(f"\nGenerated variants for {len(MIDI_NOTES)} notes:")
    for i, note in enumerate(MIDI_NOTES, 1):
        note_type = " (♯/♭)" if note.endswith('s') else ""
        print(f"  {i:2d}. {note}{note_type}")

if __name__ == "__main__":
    buttons_dir = r"d:\WansMusicRecorder\src\main\resources\Common\UI\Custom\Pages\WansMusicRecorder\Buttons"
    generate_all_variants(buttons_dir)
