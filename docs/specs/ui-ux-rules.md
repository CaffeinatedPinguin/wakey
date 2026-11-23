# UI/UX Rules — Web & PWA

> Przy sprzeczności wygrywa `ui-ux.md`, potem `theme.md`.
>
> Mandatory UI/UX rules for reusable product UI.
>
> Target:
> - React web application
> - installable PWA
> - desktop + mobile
> - dark + light themes
> - mouse + keyboard + touch
>
> These rules describe product/application UI, not marketing pages.

## Contents

- [Document ownership](#document-ownership)
- [1. Rule priority](#1-rule-priority)
- [2. Language](#2-language)
- [3. Core principles](#3-core-principles)
- [4. Theme architecture](#4-theme-architecture)
- [5. Brand accent](#5-brand-accent)
- [6. Color system](#6-color-system)
- [7. Dark theme](#7-dark-theme)
- [8. Light theme](#8-light-theme)
- [9. Layout](#9-layout)
- [10. Spacing system](#10-spacing-system)
- [11. Density](#11-density)
- [12. Typography](#12-typography)
- [13. Shapes and radius](#13-shapes-and-radius)
- [14. Surfaces and borders](#14-surfaces-and-borders)
- [15. Cards](#15-cards)
- [16. Buttons](#16-buttons)
- [17. Touch targets](#17-touch-targets)
- [18. Icons](#18-icons)
- [19. Navigation](#19-navigation)
- [20. Responsive design](#20-responsive-design)
- [21. Input modality](#21-input-modality)
- [22. PWA/mobile](#22-pwamobile)
- [23. Forms](#23-forms)
- [24. Progressive disclosure](#24-progressive-disclosure)
- [25. Tables and data-heavy UI](#25-tables-and-data-heavy-ui)
- [26. Loading, empty and error states](#26-loading-empty-and-error-states)
- [27. Interaction states](#27-interaction-states)
- [28. Focus and keyboard](#28-focus-and-keyboard)
- [29. Accessibility](#29-accessibility)
- [30. Motion](#30-motion)
- [31. UX writing](#31-ux-writing)
- [32. AI anti-patterns](#32-ai-anti-patterns)
- [33. Design tokens](#33-design-tokens)
- [34. Mantine](#34-mantine)
- [35. Required quality matrix](#35-required-quality-matrix)
- [36. Final rule](#36-final-rule)

## Document ownership

This document owns visual semantics, intent and accessibility constraints. Product behaviour belongs
to `ui-ux.md`. Token names and implemented values belong to `theme.md` and the theme code. Precisely
depicted approved features follow their reference; specified features without screenshots inherit the
same visual and interaction rules.

Do not maintain duplicate full palettes here. The canonical brand accent below is a product identity
constraint. Concrete implemented token values live in the theme package.

## 1. Rule priority

When rules conflict, use this priority:

1. Usability
2. Accessibility
3. User task
4. Existing product consistency
5. Responsive behavior
6. Visual hierarchy
7. Aesthetics
8. Decoration

A prettier solution MUST NOT make the interface harder to understand or use.

## 2. Language

- **MUST** — required
- **SHOULD** — default unless there is a concrete reason not to
- **MAY** — optional
- **NEVER** — prohibited unless explicitly requested

## 3. Core principles

- Design for the user task first.
- Every screen MUST have a clear primary purpose.
- Preserve established information architecture unless redesign is explicitly requested.
- Prefer clarity over decoration.
- Prefer recognition over recall.
- Use existing interaction patterns before inventing new ones.
- Do not add functionality merely to fill visual space.
- Refinement is not redesign.

## 4. Theme architecture

The application MUST fully support both dark and light themes.

Dark theme is the primary visual reference, but light theme is a first-class implementation.

Components MUST consume semantic tokens rather than hard-coded theme-specific colors.

Theme switching MUST NOT change:
- layout
- spacing
- component sizes
- navigation
- information hierarchy
- responsive behavior
- DOM structure solely for theme reasons

Only semantic visual tokens should change.

## 5. Brand accent

Canonical brand accent:

`#7367F0`

Use it primarily for:
- primary actions
- selected/active state
- links
- focus/accent states
- important interactive emphasis

Do not use purple on every icon, heading, badge, border and surface.

The exact brand color does not have to be used for every foreground/background combination. Create accessible semantic variants when necessary.

## 6. Color system

Use semantic tokens such as:

- `canvas`
- `surface`
- `surface-raised`
- `surface-subtle`
- `text-primary`
- `text-secondary`
- `text-muted`
- `border-subtle`
- `border-default`
- `border-strong`
- `accent`
- `accent-hover`
- `accent-subtle`
- `success`
- `warning`
- `danger`
- `info`

Do not communicate meaning with color alone.

Semantic states should use appropriate combinations of:
- color
- text
- icon
- shape/status label

## 7. Dark theme

Dark theme SHOULD use charcoal/dark-neutral surfaces, not pure black.

Hierarchy should rely primarily on:
- surface luminance
- typography
- spacing
- subtle borders

Shadows are secondary.

NEVER default to:
- neon glow
- glassmorphism
- large gradients
- black background + bright floating cards
- purple on every active element

## 8. Light theme

Light theme SHOULD use:
- slightly off-white canvas
- white/near-white surfaces
- subtle neutral borders
- dark neutral primary text
- readable secondary text
- restrained shadows

Do not mechanically invert dark colors.

## 9. Layout

Use the simplest layout that supports the task.

Typical application content SHOULD generally stay within approximately `1200–1280px`, unless the screen benefits from additional width.

Text-heavy content should use narrower readable columns.

Default page gutters:
- mobile: `16px`
- large mobile: `16–20px`
- tablet: `20–24px`
- desktop: `24–32px`

Content MUST NOT touch viewport edges.

## 10. Spacing system

Default spacing scale:

- `4px`
- `8px`
- `12px`
- `16px`
- `24px`
- `32px`
- `48px`
- `64px`
- `96px`

Prefer values from the scale.

Typical meaning:
- `4–8px`: tightly related content
- `8–12px`: internal component relationships
- `16px`: standard component spacing
- `24px`: separate groups
- `32px`: major groups
- `48px+`: sections / major separation

Spacing communicates relationships.

## 11. Density

This is a compact product UI.

Desktop MAY be dense.

Mobile MUST remain touch-friendly.

Typical:
- compact desktop row: `36–44px`
- mobile/touch row or hit area: `44–52px`

Do not use marketing-page whitespace in application screens.

## 12. Typography

Typography MUST establish obvious hierarchy.

Recommended product scale:
- metadata/caption: `12–13px`
- secondary UI: `14px`
- label: `14px`
- body: `16px`
- large body: `18px`
- small heading: `18px`
- section heading: `20–24px`
- page heading: `28–32px`
- large display: `36–48px` only when truly needed

Normal body:
- `16px`
- line-height around `1.4–1.6`

Text-heavy content:
- approximately `45–75` characters per line

Avoid important UI text at `10–11px`.

Mobile MUST NOT simply reduce all typography sizes.

## 13. Shapes and radius

Recommended scale:
- `4px`
- `6px`
- `8px`
- `12px`
- `16px`
- `999px` only for intentional pills/circles

Typical defaults:
- button: `6–8px`
- input: `6–8px`
- card: `8–12px`
- popover/dropdown: `8–12px`
- modal/sheet: `12–16px`

Do not make every component `16–24px` rounded.

## 14. Surfaces and borders

Prefer hierarchy through:

1. surface
2. spacing
3. typography
4. border
5. shadow

Avoid:
- cards inside cards
- borders around every group
- simultaneous heavy border + shadow + background changes

Three surface levels are usually enough:
- canvas
- normal surface
- raised surface

## 15. Cards

Cards are not the default container for everything.

For lists/settings/navigation groups, prefer:
- one surface
- rows
- separators
- spacing

Use cards when content is meaningfully independent.

Never automatically convert mobile rows into cards.

## 16. Buttons

Keep a small hierarchy:
- Primary
- Secondary
- Tertiary/Subtle
- Ghost
- Danger
- Icon button

Recommended heights:
- compact desktop: `32–36px`
- default desktop: `40–44px`
- default mobile/touch: `44–48px`
- large CTA: `48–52px`

A normal screen SHOULD usually have one primary action.

Use concrete labels:
- `Create task`
- `Create note`
- `Append`
- `Delete`

Avoid vague `OK` / `Proceed` if the action can be named.

## 17. Touch targets

Preferred touch target:

`>= 44 × 44px`

The visible icon may be smaller, but its hit area must remain comfortable.

Do not create tiny mobile click targets.

## 18. Icons

Use one coherent icon family.

Typical sizes:
- `16px`
- `18px`
- `20px`
- `24px`

Icon-only controls require accessible names.

Do not replace clear labels with ambiguous icons just to reduce density.

## 19. Navigation

Navigation MUST show:
- current location
- available destinations
- hierarchy

Desktop may use sidebar navigation.

Mobile uses a deliberately different layout when appropriate.

Do not shrink desktop navigation into mobile.

Active desktop navigation uses a subtle accent-tinted background, readable accent text/icon and a clear
selection indicator. Do not default to a fully saturated primary-action block.
Fix foreground/indicator contrast first, then adjust tint as needed. A stronger fill needs a concrete
accessibility justification. Selected state and keyboard focus must remain distinguishable.
Feature screenshots must reuse the canonical shell instead of introducing another navigation treatment.

## 20. Responsive design

Responsive design means adaptation, not scaling.

Desktop and mobile MAY have different presentation components, information density and secondary ordering.
They MUST share domain rules, validation, API contracts, queries, mutations and the persistent draft model.
CSS alone remains appropriate for simple layout differences. Do not force a single branch-heavy JSX tree
or duplicate the domain implementation.

When width decreases:

1. Preserve primary content/action/navigation.
2. Reflow.
3. Reposition.
4. Simplify secondary presentation.
5. Hide only truly secondary information.

Useful default ranges:
- `<480px`: small mobile
- `480–767px`: mobile
- `768–1023px`: tablet/compact
- `1024–1279px`: desktop
- `>=1280px`: wide desktop

Breakpoints are defaults, not device definitions.

Important viewport checks:
- `320px`
- `375px`
- `390px`
- `430px`
- `768px`
- `1024px`
- `1280px`
- `1440px+`

## 21. Input modality

Viewport width does not define input method.

Support:
- keyboard
- mouse
- touch

Hover may enhance interactions but MUST NOT be required for essential functionality.

## 22. PWA/mobile

Mobile/PWA MUST account for:
- safe areas
- virtual keyboard
- dynamic viewport height
- standalone mode
- portrait/landscape
- no hover dependency

Prefer modern viewport units where appropriate:
- `dvh`
- `svh`
- `lvh`

Fixed bottom UI MUST reserve content space and respect safe areas.

## 23. Forms

Labels must remain visible.

Placeholder is not a label.

Errors should explain:
1. what is wrong
2. where
3. how to fix it

Preserve entered values after validation failure.

Do not prompt on clean close/cancel. Protect genuinely dirty drafts only when an action would discard them.
Expanding optional fields without changing values does not justify a confirmation.

Mobile form controls should generally be full width when that improves usability.

Mobile input text should generally be around `16px`.

## 24. Progressive disclosure

Use progressive disclosure for optional/infrequent controls.

Three levels:

1. frequent/task-critical → directly visible
2. optional/secondary → one interaction away
3. rare/advanced → overflow/context menu

Configured optional properties SHOULD become visible.

Unused optional properties SHOULD NOT permanently consume space.

Do not hide frequent functionality behind `...`.

## 25. Tables and data-heavy UI

Do not automatically convert tables to cards on mobile.

Choose intentionally:
- horizontal scroll when comparison structure must be preserved
- reduced/prioritized columns for record lists
- responsive list representation when that improves comprehension

## 26. Loading, empty and error states

Important screens/components should consider:
- default
- loading
- partial loading
- empty
- no results
- error
- offline/timeout where relevant
- permission denied where relevant

Loading should match the operation:
- skeleton for known content shape
- spinner for small independent actions
- progress when measurable

Error states should provide a recovery path.

## 27. Interaction states

Interactive components MUST consider relevant states:
- default
- hover
- focus
- active/pressed
- selected
- disabled
- loading
- error

Do not implement only the screenshot state.

## 28. Focus and keyboard

Focus MUST always be visible.

Never remove focus outline without a visible replacement.

Core functionality MUST be keyboard accessible.

Expected conventions:
- Tab: move between controls
- Enter/Space: activate relevant controls
- Escape: dismiss appropriate overlays

## 29. Accessibility

Target: WCAG 2.2 AA.

Contrast:
- normal text: `>= 4.5:1`
- large text: `>= 3:1`
- important non-text UI boundaries/states: `>= 3:1`

Use semantic HTML first.

ARIA supplements semantics; it does not replace correct HTML.

UI must remain usable with zoom/reflow.

## 30. Motion

Motion must communicate something:
- state transition
- spatial relationship
- feedback
- enter/exit

Typical durations:
- micro: `100–150ms`
- standard: `150–250ms`
- larger transition: `250–400ms`

Prefer animating:
- `transform`
- `opacity`

Respect `prefers-reduced-motion`.

## 31. UX writing

Use clear, task-oriented language.

Prefer:
- `Create task`
- `Create note`
- `Append`
- `Delete project`

Avoid implementation language.

Use one term consistently for one concept.

## 32. AI anti-patterns

NEVER add these automatically:

- dashboard stats without product need
- cards everywhere
- cards inside cards
- glassmorphism
- neon glow
- random gradients
- giant rounded containers
- giant hero areas inside applications
- decorative blobs
- gradient text
- pill-shaped everything
- excessive shadows
- fake statistics
- excessive badges
- unnecessary chevrons
- decorative charts
- duplicate actions
- oversized whitespace
- colored icon tiles for every section

Do not invent content to fill space.

## 33. Design tokens

Visual decisions SHOULD be represented as tokens.

At minimum:
- color
- spacing
- radius
- typography
- elevation
- motion
- breakpoints
- z-index

Application components SHOULD consume semantic tokens.

## 34. Mantine

When Mantine is used:

- map semantic design tokens into Mantine theme/CSS variables using the project's token naming
- reuse Mantine primitives when appropriate
- avoid hard-coded theme colors in feature components
- reuse existing variants before creating new ones
- preserve Mantine accessibility behavior
- create app abstractions only when they provide repeated meaningful behavior
- use colocated CSS Modules for custom shell/feature styles and Mantine Styles API for library components
- keep native Mantine variables; do not introduce custom roles into the library namespace

## 35. Required quality matrix

Important screens/components should be checked for:

- desktop dark
- desktop light
- mobile dark
- mobile light
- keyboard
- touch
- focus
- loading where applicable
- empty where applicable
- error where applicable
- disabled where applicable

## 36. Final rule

A component is not finished because its desktop dark screenshot looks good.

It is finished when it:
- supports the user task
- works on desktop
- works on mobile
- works with keyboard
- works with touch
- works in dark
- works in light
- handles realistic content
- handles relevant states
- remains accessible
- remains consistent with the product
