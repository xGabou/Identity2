import os

def fix_ability_generics_and_imports():
    folder_path = os.path.dirname(os.path.abspath(__file__))  # Automatically use the script's own folder

    for root, dirs, files in os.walk(folder_path):
        for file in files:
            if file.endswith("Ability.java"):
                ability_path = os.path.join(root, file)
                entity_base_name = file.replace("Ability.java", "")  # Example: DropBear
                entity_name = f"Entity{entity_base_name}"             # Example: EntityDropBear

                with open(ability_path, "r", encoding="utf-8") as f:
                    content = f.read()

                # 1. Add import line after package and existing imports
                package_line_end = content.find(";\n") + 2
                import_line = f"import com.github.alexthe666.alexsmobs.entity.{entity_name};\n"
                content = content[:package_line_end] + import_line + content[package_line_end:]

                # 2. Replace IdentityAbility<Object> with IdentityAbility<EntityXXX>
                content = content.replace("IdentityAbility<Object>", f"IdentityAbility<{entity_name}>")

                # 3. Replace onUse parameter (Object identity) with (EntityXXX identity)
                content = content.replace("Object identity", f"{entity_name} identity")

                with open(ability_path, "w", encoding="utf-8") as f:
                    f.write(content)

                print(f"Fixed {file} -> uses {entity_name}")

if __name__ == "__main__":
    fix_ability_generics_and_imports()
