## Mod authors guide
There are two ways for mod authors to define hunger restoration and saturation values for their food items.

Both methods are optional, if none is defined, the mod will fallback to the default configuration values based on the food tier. All tiers from Common to Unique are supported.

Both methods are friendly with users of your mod that don't have this mod installed, so you can safely include them in your asset pack without affecting those users.


## Method 1: Using InteractionVars on your item

This method is prefered if you own the item asset and can modify it directly.

You should open your food item asset JSON file, and locate the `InteractionVars` section.

Then, you should add the following configuration:
```json
{
  "InteractionVars": {
    "Hungry_Feed": {
      "Interactions": [
        {
          "Type": "Hungry_Feed",
          "HungerRestoration": 60,
          "MaxHungerSaturation": 30
        }
      ]
    }
  }
}
```

You can adjust the `HungerRestoration` and `MaxHungerSaturation` values as needed.

If you prefer your items to infer the values from a template, you can create template Item assets, and then have your food items inherit from them by specifying the parent:
```json
{
  "Parent": "Template_MyMod_T1"
}
```
Hytale will merge interaction vars from the parent template into the child item, so your food items will inherit the hunger restoration and saturation values defined in the template.


## Method 2: Creating a custom asset FoodValue

This method is prefered if you don't own the item asset, for example if you're developing a hunger balance mod.



First you should create a new JSON asset under `Server/Item/Hungry/FoodValues`. The name of the JSON file must be the same as the name of your food item asset.

Then, you should define the following structure:
```json
{
  "HungerRestoration": 60,
  "MaxHungerSaturation": 30
}
```
You can adjust the `HungerRestoration` and `MaxHungerSaturation` values as needed.

This new asset support inheritance, so you can create a template asset for each one of your food tiers, and then have your food items inherit from one of them.

To do so, simply create a new template asset, like `Server/Item/Hungry/FoodValues/Template_MyMod_T1.json`, define the `HungerRestoration` value, and then have your food items inherit from that template by specifying the parent:
```json
{
  "Parent": "Template_MyMod_T1"
}
```

## Food values load order

When determining the hunger restoration and saturation values for a food item, the mod will follow this preference order:
1. Per-item user-defined values in their FoodValuesConfig file
2. Values defined by the `FoodValue` asset (Method 2)
3. Values defined in the `InteractionVars` of the item asset (Method 1)
4. User-defined configuration values based on the food tier

The reasoning behind this order is to give users the most control over their experience, while still allowing mod authors to define sensible defaults for their food items.