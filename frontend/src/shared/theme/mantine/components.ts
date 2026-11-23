import {
  type ActionIconProps,
  type ButtonProps,
  Menu,
  type MantineTheme,
  type MantineThemeOverride,
  rem,
} from '@mantine/core'
import {
  dropdownStyles,
  inputSectionStyles,
  inputStyles,
  inputWrapperStyles,
  optionStyles,
  overlayStyles,
  overlayTransition,
} from './styles'
import type { AppThemeLabels } from './types'
import type { ControlVars } from './variant'

const surfaceComponents = {
  Paper: {
    defaultProps: { radius: 'lg' },
    styles: {
      root: {
        background: 'var(--app-surface-item)',
        borderColor: 'var(--app-color-border-soft)',
      },
    },
  },
  Card: {
    defaultProps: { radius: 'lg', padding: 'md' },
    styles: {
      root: {
        background: 'var(--app-color-surface)',
        borderColor: 'var(--app-color-border-soft)',
      },
    },
  },
  Badge: {
    defaultProps: { variant: 'light', size: 'md' },
    styles: {
      root: {
        height: 'var(--app-size-badge)',
        paddingInline: rem(8),
        textTransform: 'none',
        letterSpacing: 'normal',
        fontWeight: 600,
        fontSize: 'var(--app-type-badge-size)',
        fontVariantNumeric: 'tabular-nums',
        borderRadius: 'var(--app-radius-pill)',
      },
    },
  },
  ThemeIcon: { defaultProps: { variant: 'light', radius: 'md' } },
  Title: { styles: { root: { color: 'var(--app-color-text-heading)' } } },
  Skeleton: { defaultProps: { radius: 'sm' } },
  Divider: { styles: { root: { borderColor: 'var(--app-color-border-soft)' } } },
  Code: {
    styles: {
      root: {
        fontSize: 'var(--app-type-mono-size)',
        borderRadius: 'var(--app-radius-sm)',
        background: 'var(--app-color-surface-subtle)',
        color: 'var(--app-color-text-heading)',
      },
    },
  },
  Anchor: { styles: { root: { color: 'var(--app-color-accent-text)' } } },
}

const formComponents = {
  Input: {
    styles: { wrapper: inputWrapperStyles, input: inputStyles, section: inputSectionStyles },
  },
  TextInput: { defaultProps: { size: 'sm' } },
  PasswordInput: { defaultProps: { size: 'sm' } },
  NumberInput: {
    defaultProps: { size: 'sm' },
    styles: { input: { fontVariantNumeric: 'tabular-nums' } },
  },
  Select: {
    defaultProps: {
      size: 'sm',
      checkIconPosition: 'right',
      comboboxProps: { position: 'bottom-start' },
    },
    styles: { dropdown: dropdownStyles, option: optionStyles },
  },
  MultiSelect: {
    defaultProps: { size: 'sm', comboboxProps: { position: 'bottom-start' } },
    styles: { dropdown: dropdownStyles, option: optionStyles },
  },
  Textarea: { defaultProps: { size: 'sm', autosize: true, minRows: 3 } },
  InputWrapper: {
    styles: {
      label: {
        fontSize: 'var(--app-type-body-size)',
        lineHeight: 'var(--app-type-body-line)',
        fontWeight: 500,
        color: 'var(--app-color-text-heading)',
        marginBottom: rem(4),
      },
      description: {
        fontSize: 'var(--app-type-metadata-size)',
        lineHeight: 'var(--app-type-metadata-line)',
        color: 'var(--app-color-text-muted)',
      },
      error: {
        fontSize: 'var(--app-type-metadata-size)',
        lineHeight: 'var(--app-type-metadata-line)',
        color: 'var(--app-color-danger-text)',
      },
    },
  },
  Checkbox: { defaultProps: { radius: 'sm', size: 'sm' } },
  Radio: { defaultProps: { variant: 'outline', size: 'sm' } },
  Switch: { defaultProps: { size: 'sm' } },
  SegmentedControl: { defaultProps: { radius: 'md', size: 'sm' } },
  Combobox: {
    defaultProps: { position: 'bottom-start' },
    styles: { dropdown: dropdownStyles, option: optionStyles },
  },
}

const dataComponents = {
  Table: {
    defaultProps: { horizontalSpacing: 'md', verticalSpacing: 'xs', highlightOnHover: true },
    styles: {
      table: {
        fontVariantNumeric: 'tabular-nums',
        '--table-border-color': 'var(--app-color-border-soft)',
        '--table-hover-color': 'var(--app-color-interactive-hover)',
      },
      th: {
        fontSize: 'var(--app-type-metadata-size)',
        fontWeight: 600,
        color: 'var(--app-color-text-muted)',
        whiteSpace: 'nowrap',
      },
    },
  },
  Pagination: { defaultProps: { size: 'sm', radius: 'md' } },
  Tooltip: {
    defaultProps: { multiline: true, openDelay: 300 },
    styles: {
      tooltip: {
        maxWidth: 'min(var(--app-size-tooltip-max), var(--app-size-overlay-available))',
        padding: 'var(--app-space-8) var(--app-space-12)',
        background: 'var(--app-color-surface-raised)',
        color: 'var(--app-color-text-heading)',
        border: '1px solid var(--app-color-border-soft)',
        borderRadius: 'var(--app-radius-control)',
        boxShadow: 'var(--app-shadow-popover)',
        fontSize: 'var(--app-type-metadata-size)',
        lineHeight: 'var(--app-type-metadata-line)',
        overflowWrap: 'anywhere',
      },
    },
  },
  Notification: { defaultProps: { radius: 'lg' } },
}

function buttonComponent(controlVars: ControlVars) {
  return {
    defaultProps: { size: 'sm', radius: 'md' },
    vars: (_theme: MantineTheme, props: ButtonProps) => ({
      root: controlVars(props.variant, props.color),
    }),
    styles: {
      root: {
        '--button-height-xs': 'var(--app-size-control-compact)',
        '--button-height-sm': 'var(--app-size-control-desktop)',
        '--button-height-md': 'var(--app-size-control-large)',
        '--button-height-lg': 'var(--app-size-control-touch)',
        '--button-height-xl': 'var(--app-size-control-critical)',
        '--button-height-compact-xs': 'var(--app-size-control-compact)',
        '--button-height-compact-sm': 'var(--app-size-control-compact)',
        '--button-height-compact-md': 'var(--app-size-control-compact)',
        '--button-height-compact-lg': 'var(--app-size-control-desktop)',
        '--button-height-compact-xl': 'var(--app-size-control-large)',
        '--button-padding-x-xs': 'var(--app-space-8)',
        '--button-padding-x-sm': 'var(--app-space-control-inline)',
        '--button-padding-x-md': 'var(--app-space-16)',
        '--button-padding-x-lg': 'var(--app-space-16)',
        '--button-padding-x-xl': 'var(--app-space-24)',
        '--button-padding-x-compact-xs': 'var(--app-space-8)',
        '--button-padding-x-compact-sm': 'var(--app-space-8)',
        '--button-padding-x-compact-md': 'var(--app-space-8)',
        fontSize: 'var(--app-type-body-size)',
        fontWeight: 'var(--app-type-control-weight)',
        lineHeight: 'var(--app-type-body-line)',
      },
      section: { marginInlineEnd: 'var(--app-space-control-gap)' },
    },
  }
}

function actionIconComponent(controlVars: ControlVars) {
  return {
    defaultProps: { variant: 'subtle', color: 'gray', size: 'md', radius: 'md' },
    vars: (_theme: MantineTheme, props: ActionIconProps) => ({
      root: controlVars(props.variant, props.color),
    }),
    styles: {
      root: {
        // Minimum 24 px (WCAG 2.5.8); on touch base.css raises everything to 44 px.
        '--ai-size-xs': rem(24),
        '--ai-size-sm': 'var(--app-size-control-compact)',
        '--ai-size-md': 'var(--app-size-control-desktop)',
        '--ai-size-lg': 'var(--app-size-control-touch)',
        '--ai-size-xl': 'var(--app-size-control-critical)',
      },
    },
  }
}

const menuComponent = Menu.extend({
  defaultProps: { shadow: 'md', radius: 'lg', transitionProps: overlayTransition },
  styles: {
    dropdown: dropdownStyles,
    label: {
      color: 'var(--app-color-text-muted)',
      fontSize: 'var(--app-type-section-label-size)',
      fontWeight: 600,
      letterSpacing: 'var(--app-type-section-label-tracking)',
      textTransform: 'uppercase',
    },
    item: {
      '--menu-item-hover': 'var(--app-color-interactive-hover)',
      color: 'var(--app-color-text-body)',
      minHeight: 'var(--app-size-menu-item)',
      fontSize: 'var(--app-type-body-size)',
      fontWeight: 500,
      borderRadius: 'var(--app-radius-control)',
    },
    itemSection: { color: 'var(--app-color-text-muted)' },
  },
})

function controlComponents(controlVars: ControlVars) {
  return {
    Button: buttonComponent(controlVars),
    ActionIcon: actionIconComponent(controlVars),
    Menu: menuComponent,
  }
}

function overlayComponents(closeButtonProps: { 'aria-label': string }) {
  return {
    Popover: {
      defaultProps: { radius: 'lg', shadow: 'md', transitionProps: overlayTransition },
      styles: { dropdown: dropdownStyles },
    },
    Drawer: {
      defaultProps: { padding: 'md', radius: 'xl', closeButtonProps },
      styles: overlayStyles,
    },
    Modal: {
      defaultProps: {
        size: 'var(--app-size-modal-default)',
        radius: 'xl',
        padding: 'md',
        centered: true,
        closeButtonProps,
      },
      styles: {
        ...overlayStyles,
        content: { ...overlayStyles.content, maxWidth: 'var(--app-size-overlay-available)' },
      },
    },
  }
}

export function buildComponents(
  controlVars: ControlVars,
  labels: AppThemeLabels,
): NonNullable<MantineThemeOverride['components']> {
  const closeButtonProps = { 'aria-label': labels.close }
  return {
    ...surfaceComponents,
    ...formComponents,
    ...dataComponents,
    ...controlComponents(controlVars),
    ...overlayComponents(closeButtonProps),
  }
}
